package dev.agustin.serializer;
import java.io.*;
import java.util.Enumeration;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FilenameUtils;

import com.threed.jpct.*;
import javax.swing.JWindow;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Dialog;
import java.awt.Panel;
import java.awt.GridBagLayout;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class MainWindow extends JFrame {

	private javax.swing.JPanel jContentPane = null;
	private javax.swing.JPanel jPanel = null;
	private javax.swing.JButton jButton = null;
	private javax.swing.JButton jButton1 = null;
	private javax.swing.JButton jButton2 = null;
	private javax.swing.JScrollPane jScrollPane = null;
	private javax.swing.JTextArea jTextArea = null;
	private javax.swing.JFileChooser jFileChooser = null;  //  @jve:decl-index=0:visual-constraint="682,33"
	private boolean hasChanged = false;
	private static final String title = "3D Engine Object Serializer";
	private Object3D[] objectArray;
	private String saveDirectory;

	OutputStream consoleOutput = new OutputStream() {  
		@Override  
		public void write(final int b) throws IOException {  
			getJTextArea().append(String.valueOf((char) b));  //  @jve:decl-index=0:
			textScroll();
		}  };
		/**
		 * This method initializes 
		 * 
		 */
		public MainWindow() {
			super();
			initialize();
			setOutputStreamToScreen();
			System.out.println("3d Engine converter.");
			System.out.println("Copyright Agustin Sanchez 2010 all rights reserved");
			System.out.println("Press the \"Load 3DS/OBJ\" button to load a 3D Studio file to memory.");
			System.out.println("Press the \"Serialize\" button to save the file to 3D Engine format.");
			System.out.println("The program will create one file for each object found in the 3ds file.");
			System.out.println("Files will be named after the found 3ds objects");
			System.out.println("Ready.");
		}

		protected void textScroll() {
			SwingUtilities.invokeLater(new Runnable() {
		        public void run() {
		        	getJTextArea().setCaretPosition(getJTextArea().getText().length());
		        }
		      });

			
		}

		private void setOutputStreamToScreen() {
			PrintStream str = new PrintStream(consoleOutput, true);
			System.setOut(str);
			System.setErr(str);
		}

		public static void main(String[] args) {
			MainWindow ste = new MainWindow();
			ste.show();
		}

		/**
		 * This method initializes jContentPane
		 * 
		 * @return javax.swing.JPanel
		 */
		private javax.swing.JPanel getJContentPane() {
			if (jContentPane == null) {
				jContentPane = new javax.swing.JPanel();
				jContentPane.setLayout(new java.awt.BorderLayout());
				jContentPane.add(getJPanel(), java.awt.BorderLayout.SOUTH);
				jContentPane.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
				jContentPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(
						5, 5, 5, 5));
			}
			return jContentPane;
		}

		/**
		 * This method initializes this
		 * 
		 * @return void
		 */
		private void initialize() {
			this.setContentPane(getJContentPane());
			this.setSize(616, 385);
			this.setTitle(title);
			this
			.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
			this.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent e) {
					doExit();
				}
			});

		}

		/**
		 * This method initializes jPanel
		 * 
		 * @return javax.swing.JPanel
		 */
		private javax.swing.JPanel getJPanel() {
			if (jPanel == null) {
				jPanel = new javax.swing.JPanel();
				jPanel.add(getJButton(), null);
				jPanel.add(getJButton1(), null);
				jPanel.add(getJButton2(), null);
			}
			return jPanel;
		}

		/**
		 * This method initializes jButton
		 * 
		 * @return javax.swing.JButton
		 */
		private javax.swing.JButton getJButton() {
			if (jButton == null) {
				jButton = new javax.swing.JButton();
				jButton.setText("Load 3DS/OBJ");
				jButton.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						load3dFile();
					}
				});
			}
			return jButton;
		}

		/**
		 * This method initializes jButton1
		 * 
		 * @return javax.swing.JButton
		 */
		private javax.swing.JButton getJButton1() {
			if (jButton1 == null) {
				jButton1 = new javax.swing.JButton();
				jButton1.setText("Serialize");
				jButton1.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						saveJpctFile();
					}
				});
			}
			return jButton1;
		}

		/**
		 * This method initializes jButton2
		 * 
		 * @return javax.swing.JButton
		 */
		private javax.swing.JButton getJButton2() {
			if (jButton2 == null) {
				jButton2 = new javax.swing.JButton();
				jButton2.setText("Exit");
				jButton2.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						doExit();
					}
				});
			}
			return jButton2;
		}

		/**
		 * This method initializes jScrollPane
		 * 
		 * @return javax.swing.JScrollPane
		 */
		private javax.swing.JScrollPane getJScrollPane() {
			if (jScrollPane == null) {
				jScrollPane = new javax.swing.JScrollPane();
				jScrollPane.setViewportView(getJTextArea());
				jScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener(){
					public void adjustmentValueChanged(AdjustmentEvent e){
						jTextArea.select(jTextArea.getHeight()+1000, 0);
					}});
			}
			return jScrollPane;
		}

		/**
		 * This method initializes jTextArea
		 * 
		 * @return javax.swing.JTextArea
		 */
		private javax.swing.JTextArea getJTextArea() {
			if (jTextArea == null) {
				jTextArea = new javax.swing.JTextArea();
				jTextArea.setEditable(false);
				jTextArea.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
					public void propertyChange(java.beans.PropertyChangeEvent e) {
						if ((e.getPropertyName().equals("text"))) {
							System.out.println("propertyChange(text)"); // TODO Auto-generated property Event stub "text"
						}
					}
				});
			}
			return jTextArea;
		}

		private class MeshFileFilter extends javax.swing.filechooser.FileFilter {

			@Override
			public String getDescription() {
				return "(.3DS) 3d Studio Files / (.OBJ) Wavefront";
			}

			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				String name = f.getName().toLowerCase();
				return name.endsWith("3ds") || name.endsWith("obj");
			}//end accept

		}//end class HTMLFileFilter

		/**
		 * This method initializes jFileChooser
		 * 
		 * @return javax.swing.JFileChooser
		 */
		private javax.swing.JFileChooser getJFileChooser() {
			if (jFileChooser == null) {
				jFileChooser = new javax.swing.JFileChooser();
				jFileChooser.setMultiSelectionEnabled(false);
				jFileChooser.setFileFilter(new MeshFileFilter());
			}
			return jFileChooser;
		}

		private void load3dFile() {
			int state = getJFileChooser().showOpenDialog(this);
			if (state == JFileChooser.APPROVE_OPTION) {
				File f = getJFileChooser().getSelectedFile();
				try {
					BufferedReader br = new BufferedReader(new FileReader(f));
					String fileExtension = FilenameUtils.getExtension(f.getName());
					this.saveDirectory = FilenameUtils.getPath(f.getPath()); 
					if (fileExtension.compareToIgnoreCase("3ds")==0)
					{
						//3ds file, load in the engine
						try
						{
							this.objectArray = Loader.load3DS(f.getCanonicalPath(), 1);
						}
						catch (Exception ex)
						{
							System.out.print(ex.getMessage());
						}
					}
					else if (fileExtension.compareToIgnoreCase("obj")==0)
					{
						//obj file
						try
						{
							String mtlFile = f.getCanonicalPath().replace(".obj", ".mtl");
							this.objectArray = Loader.loadOBJ(f.getCanonicalPath(), mtlFile, 1);
						}
						catch (Exception ex)
						{
							System.out.println("ERROR LOADING MATERIAL! trying without material");
							System.out.print(ex.getMessage());
							try
							{
								this.objectArray = Loader.loadOBJ(f.getCanonicalPath(), null, 1);
							}
							catch (Exception ex2)
							{
								System.out.println("Error loading object, aborting.");
								System.out.print(ex.getMessage());
							}
						}
					}
					else
					{
						System.out.println("Error loading file " + f.getName() + " not a 3D Studio (.3DS) file");
					}
					br.close();
					setTitle(title);
					hasChanged = false;
					System.out.println("Now dumping textures names:");
					Enumeration<String> textures = TextureManager.getInstance().getNames();
					while (textures.hasMoreElements())
					{
						System.out.println(textures.nextElement());
					}
					System.out.println("Done.");
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		private void saveJpctFile()
		{
			try
			{
				System.out.println("Building all objects...");
				for (int i=0; i<this.objectArray.length;i++)
				{
					Object3D obj = (Object3D)this.objectArray[i];
					String outputFile = "/" + this.saveDirectory + obj.getName() + ".jpct";
					System.out.println("Persisting object " + obj.getName() + " as file " + outputFile);
					FileOutputStream fout = new FileOutputStream(outputFile);
					obj.build();
					System.out.println("Object " + obj.getName() + " built.");
					DeSerializer de = new DeSerializer();
					de.serialize(obj, fout, true);
					fout.close();
				}
				System.out.println("Serializing object array as a single file... (objectarray.jpct)");
				String outputFile = "/" + this.saveDirectory + "objectarray.jpct";
				FileOutputStream fout = new FileOutputStream(outputFile);
				DeSerializer de = new DeSerializer();
				de.serializeArray(objectArray, fout, true);
				fout.close();
				System.out.println("Done.");
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		private void doExit() {
			if (hasChanged) {
				int state = JOptionPane.showConfirmDialog(this,
				"File has been changed. Save before exit?");
				if (state == JOptionPane.YES_OPTION) {
					saveJpctFile();
				} else if (state == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
			System.exit(0);
		}
} //  @jve:visual-info  decl-index=0 visual-constraint="20,27"
