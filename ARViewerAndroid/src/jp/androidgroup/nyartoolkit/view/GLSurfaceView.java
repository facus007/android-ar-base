/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.androidgroup.nyartoolkit.view;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import dev.agustin.BaseRenderer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * An implementation of SurfaceView that uses the dedicated surface for
 * displaying an OpenGL animation.  This allows the animation to run in a
 * separate thread, without requiring that it be driven by the update mechanism
 * of the view hierarchy.
 *
 * The application-specific rendering code is delegated to a GLView.Renderer
 * instance.
 */
public class GLSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTUOUSLY = 1;

    /**
     * Check glError() after every GL call.
     */
    public final static int DEBUG_CHECK_GL_ERROR = 1;

    /**
     * Log GL calls to the system log at "verbose" level with tag "GLSurfaceView".
     */
    public final static int DEBUG_LOG_GL_CALLS = 2;

    public GLSurfaceView(Context context) {
        super(context);
        init();
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }

    /**
     * Set the glWrapper to a new value. The current glWrapper is used
     * whenever a surface is created. The default value is null.
     * @param glWrapper the new GLWrapper
     */
    public void setGLWrapper(GLWrapper glWrapper) {
        mGLWrapper = glWrapper;
    }

    /**
     * Set the debug flags to a new value. The debug flags take effect
     * whenever a surface is created. The default value is zero.
     * @param debugFlags the new debug flags
     * @see #DEBUG_CHECK_GL_ERROR
     * @see #DEBUG_LOG_GL_CALLS
     */
    public void setDebugFlags(int debugFlags) {
        mDebugFlags = debugFlags;
    }

    public int getDebugFlags() {
        return mDebugFlags;
    }

    /**
     * Set the renderer associated with this view. Can only be called once.
     * @param mRenderer
     */
    public void setRenderer(BaseRenderer mRenderer) {
        if (mGLThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
        if (mEGLConfigChooser == null) {
            mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        mGLThread = new GLThread((Renderer) mRenderer);
        mGLThread.start();
    }

    /**
     * Set the EGLConfigChooser associated with this view. If this method is
     * called at all, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * The supplied configChooser will be used to choose a configuration.
     * @param configChooser
     */
    public void setEGLConfigChooser(EGLConfigChooser configChooser) {
        if (mGLThread != null) {
            throw new IllegalStateException(
                    "setRenderer has already been called for this instance.");
        }
        mEGLConfigChooser = configChooser;
    }

    /**
     * Set the EGLConfigChooser associated with this view. If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * This method installs a config chooser which will choose a config
     * as close to 16-bit RGB as possible, with or without an optional depth
     * buffer as close to 16-bits as possible.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose a config as close to 16-bit RGB as possible, with
     * a depth buffer as close to 16-bits as possible.
     *
     * @param needDepth
     */
    public void setEGLConfigChooser(boolean needDepth) {
        setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
    }

    /**
     * Set the EGLConfigChooser associated with this view. If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * This method installs a config chooser which will choose a config
     * with at least the specified component sizes, and as close
     * to the specified component sizes as possible.
     *
     */
    public void setEGLConfigChooser(int redSize, int greenSize, int blueSize,
            int alphaSize, int depthSize, int stencilSize) {
        setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize,
                blueSize, alphaSize, depthSize, stencilSize));
    }
    /**
     * Set the rendering mode. When the renderMode is
     * RENDERMODE_CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When the rendermode
     * is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when requestRender is called. Defaults to RENDERMODE_CONTINUOUSLY.
     * @param renderMode one of the RENDERMODE_X constants
     */
    public void setRenderMode(int renderMode) {
        mGLThread.setRenderMode(renderMode);
    }

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     * @return true if the renderer will render continuously.
     */
    public int getRenderMode() {
        return mGLThread.getRenderMode();
    }

    /**
     * Request that the renderer render a frame. May be called
     * from any thread. Must not be called before a renderer has been set.
     * This method is typically used when the render mode has been set to
     * false, so that frames are only rendered on demand.
     */
    public void requestRender() {
        mGLThread.requestRender();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mGLThread.surfaceCreated();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return
        mGLThread.surfaceDestroyed();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mGLThread.onWindowResize(w, h);
    }

    /**
     * Inform the view that the activity is paused. The owner of this view must
     * call this method when the activity is paused.
     */
    public void onPause() {
        mGLThread.onPause();
    }

    /**
     * Inform the view that the activity is resumed. The owner of this view must
     * call this method when the activity is resumed.
     */
    public void onResume() {
        mGLThread.onResume();
    }

    /**
     * Queue an "event" to be run on the GL rendering thread.
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) {
        mGLThread.queueEvent(r);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mGLThread.requestExitAndWait();
    }

    // ----------------------------------------------------------------------

    public interface GLWrapper {
      GL wrap(GL gl);
    }

    // ----------------------------------------------------------------------

    /**
     * A generic renderer interface.
     */
    public interface Renderer {
        /**
         * Surface created.
         * Called when the surface is created. Called when the application
         * starts, and whenever the GPU is reinitialized. This will
         * typically happen when the device awakes after going to sleep.
         * Set your textures here.
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         * @param config the EGLConfig of the created surface. Can be used
         * to create matching pbuffers.
         */
        void onSurfaceCreated(GL10 gl, EGLConfig config);
        /**
         * Surface changed size.
         * Called after the surface is created and whenever
         * the OpenGL ES surface size changes. Set your viewport here.
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         * @param width
         * @param height
         */
        void onSurfaceChanged(GL10 gl, int width, int height);
        /**
         * Draw the current frame.
         * @param gl the GL interface. Use <code>instanceof</code> to
         * test if the interface supports GL11 or higher interfaces.
         */
        void onDrawFrame(GL10 gl);
    }

    /**
     * An interface for choosing a configuration from a list of
     * potential configurations.
     *
     */
    public interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * {@link EGL10#eglChooseConfig} and iterating through the results.
         * @param egl the EGL10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        EGLConfig chooseConfig(EGL10 egl, EGLDisplay display);
    }

    private static abstract class BaseConfigChooser
            implements EGLConfigChooser {
        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = configSpec;
        }
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, mConfigSpec, null, 0, num_config);

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config);
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs);

        protected int[] mConfigSpec;
    }

    private static class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                int alphaSize, int depthSize, int stencilSize) {
            super(new int[] {
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
       }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                EGLConfig[] configs) {
            EGLConfig closestConfig = null;
            int closestDistance = 1000;
            for(EGLConfig config : configs) {
                int r = findConfigAttrib(egl, display, config,
                        EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config,
                         EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config,
                          EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config,
                        EGL10.EGL_ALPHA_SIZE, 0);
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                int distance = Math.abs(r - mRedSize)
                    + Math.abs(g - mGreenSize)
                    + Math.abs(b - mBlueSize) + Math.abs(a - mAlphaSize)
                    + Math.abs(d - mDepthSize) + Math.abs(s - mStencilSize);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestConfig = config;
                }
            }
            return closestConfig;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                EGLConfig config, int attribute, int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
        }

    /**
     * This class will choose a supported surface as close to
     * RGB565 as possible, with or without a depth buffer.
     *
     */
    private static class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(4, 4, 4, 0, withDepthBuffer ? 16 : 0, 0);
            // Adjust target values. This way we'll accept a 4444 or
            // 555 buffer if there's no 565 buffer available.
            mRedSize = 5;
            mGreenSize = 6;
            mBlueSize = 5;
        }
    }

    /**
     * An EGL helper class.
     */

    private class EglHelper {
        public EglHelper() {

        }

        /**
         * Initialize EGL for a given configuration spec.
         * @param configSpec
         */
        public void start(){
            /*
             * Get an EGL instance
             */
            mEgl = (EGL10) EGLContext.getEGL();

            /*
             * Get to the default display.
             */
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            /*
             * We can now initialize EGL for that display
             */
            int[] version = new int[2];
            mEgl.eglInitialize(mEglDisplay, version);
            mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);

            /*
            * Create an OpenGL ES context. This must be done only once, an
            * OpenGL context is a somewhat heavy object.
            */
            mEglContext = mEgl.eglCreateContext(mEglDisplay, mEglConfig,
                    EGL10.EGL_NO_CONTEXT, null);

            mEglSurface = null;
        }

        /*
         * React to the creation of a new surface by creating and returning an
         * OpenGL interface that renders to that surface.
         */
        public GL createSurface(SurfaceHolder holder) {
            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            if (mEglSurface != null) {

                /*
                 * Unbind and destroy the old EGL surface, if
                 * there is one.
                 */
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
            }

            /*
             * Create an EGL surface we can render into.
             */
            mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay,
                    mEglConfig, holder, null);

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface,
                    mEglContext);


            GL gl = mEglContext.getGL();
            if (mGLWrapper != null) {
                gl = mGLWrapper.wrap(gl);
            }

/*            if ((mDebugFlags & (DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS))!= 0) {
                int configFlags = 0;
                Writer log = null;
                if ((mDebugFlags & DEBUG_CHECK_GL_ERROR) != 0) {
                    configFlags |= GLDebugHelper.CONFIG_CHECK_GL_ERROR;
                }
                if ((mDebugFlags & DEBUG_LOG_GL_CALLS) != 0) {
                    log = new LogWriter();
                }
                gl = GLDebugHelper.wrap(gl, configFlags, log);
            }*/
            return gl;
        }

        /**
         * Display the current render surface.
         * @return false if the context has been lost.
         */
        public boolean swap() {
            mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);

            /*
             * Always check for EGL_CONTEXT_LOST, which means the context
             * and all associated data were lost (For instance because
             * the device went to sleep). We need to sleep until we
             * get a new surface.
             */
            return mEgl.eglGetError() != EGL11.EGL_CONTEXT_LOST;
        }

        public void finish() {
            if (mEglSurface != null) {
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
                mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
                mEglSurface = null;
            }
            if (mEglContext != null) {
                mEgl.eglDestroyContext(mEglDisplay, mEglContext);
                mEglContext = null;
            }
            if (mEglDisplay != null) {
                mEgl.eglTerminate(mEglDisplay);
                mEglDisplay = null;
            }
        }

        EGL10 mEgl;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        EGLConfig mEglConfig;
        EGLContext mEglContext;
    }

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     *
     */
    class GLThread extends Thread {
        GLThread(Renderer renderer) {
            super();
            mDone = false;
            mWidth = 0;
            mHeight = 0;
            mRequestRender = true;
            mRenderMode = RENDERMODE_CONTUOUSLY;
            mRenderer = renderer;
            setName("GLThread");
        }

        @Override
        public void run() {
            /*
             * When the android framework launches a second instance of
             * an activity, the new instance's onCreate() method may be
             * called before the first instance returns from onDestroy().
             *
             * This semaphore ensures that only one instance at a time
             * accesses EGL.
             */
            try {
                try {
                sEglSemaphore.acquire();
                } catch (InterruptedException e) {
                    return;
                }
                guardedRun();
            } catch (InterruptedException e) {
                // fall thru and exit normally
            }
            catch (IllegalArgumentException e)
        	{
            	Log.e("GLSurfaceView", e.getMessage());
            	e.printStackTrace();
            } finally {
                sEglSemaphore.release();
            }
        }

        private void guardedRun() throws InterruptedException {
            mEglHelper = new EglHelper();
            mEglHelper.start();

            GL10 gl = null;
            boolean tellRendererSurfaceCreated = true;
            boolean tellRendererSurfaceChanged = true;

            /*
             * This is our main activity thread's loop, we go until
             * asked to quit.
             */
            while (!mDone) {

                /*
                 *  Update the asynchronous state (window size)
                 */
                int w, h;
                boolean changed;
                boolean needStart = false;
                synchronized (this) {
                    Runnable r;
                    while ((r = getEvent()) != null) {
                        r.run();
                    }
                    if (mPaused) {
                        mEglHelper.finish();
                        needStart = true;
                    }
                    while (needToWait()) {
                        wait();
                    }
                    if (mDone) {
                        break;
                    }
                    changed = mSizeChanged;
                    w = mWidth;
                    h = mHeight;
                    mSizeChanged = false;
                    mRequestRender = false;
                }
                if (needStart) {
                    mEglHelper.start();
                    tellRendererSurfaceCreated = true;
                    changed = true;
                }
                if (changed) {
                    gl = (GL10) mEglHelper.createSurface(getHolder());
                    tellRendererSurfaceChanged = true;
                }
                if (tellRendererSurfaceCreated) {
                    mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
                    tellRendererSurfaceCreated = false;
                }
                if (tellRendererSurfaceChanged) {
                    mRenderer.onSurfaceChanged(gl, w, h);
                    tellRendererSurfaceChanged = false;
                }
                if ((w > 0) && (h > 0)) {
                    /* draw a frame here */
                    mRenderer.onDrawFrame(gl);

                    /*
                     * Once we're done with GL, we need to call swapBuffers()
                     * to instruct the system to display the rendered frame
                     */
                    mEglHelper.swap();
                }
             }

            /*
             * clean-up everything...
             */
            mEglHelper.finish();
        }

        private boolean needToWait() {
            if (mDone) {
                return false;
            }

            if (mPaused || (! mHasSurface)) {
                return true;
            }

            if ((mWidth > 0) && (mHeight > 0) && (mRequestRender || (mRenderMode == RENDERMODE_CONTUOUSLY))) {
                return false;
            }

            return true;
        }

        public void setRenderMode(int renderMode) {
            if ( !((RENDERMODE_WHEN_DIRTY <= renderMode) && (renderMode <= RENDERMODE_CONTUOUSLY)) ) {
                throw new IllegalArgumentException("renderMode");
            }
            synchronized(this) {
                mRenderMode = renderMode;
                if (renderMode == RENDERMODE_CONTUOUSLY) {
                    notify();
                }
            }
        }

        public int getRenderMode() {
            synchronized(this) {
                return mRenderMode;
            }
        }

        public void requestRender() {
            synchronized(this) {
                mRequestRender = true;
                notify();
            }
        }

        public void surfaceCreated() {
            synchronized(this) {
                mHasSurface = true;
                notify();
            }
        }

        public void surfaceDestroyed() {
            synchronized(this) {
                mHasSurface = false;
                notify();
            }
        }

        public void onPause() {
            synchronized (this) {
                mPaused = true;
            }
        }

        public void onResume() {
            synchronized (this) {
                mPaused = false;
                notify();
            }
        }

        public void onWindowResize(int w, int h) {
            synchronized (this) {
                mWidth = w;
                mHeight = h;
                mSizeChanged = true;
                notify();
            }
        }

        public void requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            synchronized(this) {
                mDone = true;
                notify();
            }
            try {
                join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         * @param r the runnable to be run on the GL rendering thread.
         */
        public void queueEvent(Runnable r) {
            synchronized(this) {
                mEventQueue.add(r);
            }
        }

        private Runnable getEvent() {
            synchronized(this) {
                if (mEventQueue.size() > 0) {
                    return mEventQueue.remove(0);
                }

            }
            return null;
        }

        private boolean mDone;
        private boolean mPaused;
        private boolean mHasSurface;
        private int mWidth;
        private int mHeight;
        private int mRenderMode;
        private boolean mRequestRender;
        private Renderer mRenderer;
        private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
        private EglHelper mEglHelper;
    }

    static class LogWriter extends Writer {

        @Override public void close() {
            flushBuilder();
        }

        @Override public void flush() {
            flushBuilder();
        }

        @Override public void write(char[] buf, int offset, int count) {
            for(int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if ( c == '\n') {
                    flushBuilder();
                }
                else {
                    mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (mBuilder.length() > 0) {
                Log.v("GLSurfaceView", mBuilder.toString());
                mBuilder.delete(0, mBuilder.length());
            }
        }

        private StringBuilder mBuilder = new StringBuilder();
    }

    private static final Semaphore sEglSemaphore = new Semaphore(1);
    private boolean mSizeChanged = true;

    private GLThread mGLThread;
    private EGLConfigChooser mEGLConfigChooser;
    private GLWrapper mGLWrapper;
    private int mDebugFlags;
}
