package edu.cmu.pandaa.desktop;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cmu.pandaa.module.AudioConversionModule;
import edu.cmu.pandaa.module.StreamModule;
import edu.cmu.pandaa.stream.FrameStream;
import edu.cmu.pandaa.stream.RawAudioFileStream;
import edu.cmu.pandaa.utils.WavUtil;

public class AudioConversionUI extends JPanel implements ActionListener {
	JButton convertWavToFrameButton, playFrameFileButton;
	JButton openWavButton, saveAsButton;
	JTextField wavFilePath, frameFilePath;
	JTextArea log;
	JFileChooser fc;
	StreamModule streamModule;
	FrameStream fs;

	public AudioConversionUI() {
		super(new BorderLayout());

		log = new JTextArea(5, 20);

		log.setMargin(new Insets(5, 5, 5, 5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		fc = new JFileChooser();

		convertWavToFrameButton = new JButton("Convert a file to Audio frames");
		convertWavToFrameButton.addActionListener(this);
		
		openWavButton = new JButton("Open Wav file");
		openWavButton.addActionListener(this);
		
		saveAsButton = new JButton("Save As");
		saveAsButton.addActionListener(this);
		
		wavFilePath = new JTextField(50);
		wavFilePath.setFocusable(false);
		frameFilePath = new JTextField(50);
		frameFilePath.setFocusable(false);

//		playFrameFileButton = new JButton("Play a file in Frame Format");
//		playFrameFileButton.addActionListener(this);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		buttonPanel.add(openWavButton,gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		buttonPanel.add(wavFilePath,gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		buttonPanel.add(saveAsButton,gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		buttonPanel.add(frameFilePath,gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill= GridBagConstraints.VERTICAL;
		buttonPanel.add(convertWavToFrameButton, gbc);
//		buttonPanel.add(playFrameFileButton);
		add(buttonPanel, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);
	}

	private void createAndShowGUI() {
		JFrame frame = new JFrame("PandaaAudioConversionUI");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.add(new AudioConversionUI());
		frame.pack();
		frame.setLocation(200, 300);
		frame.setVisible(true);
	}
	
	public void init() {
		streamModule = new AudioConversionModule();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == openWavButton) {
			fc.setFileFilter(new FileNameExtensionFilter("WAVE file (.wav)", "wav"));
			int returnVal = fc.showOpenDialog(AudioConversionUI.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				wavFilePath.setText(fc.getSelectedFile().getAbsolutePath());
			} else {
				log.append("No file selected for conversion.\n");
			}
		}
		if (e.getSource() == saveAsButton) {
			fc.setFileFilter(new FileNameExtensionFilter("Frame file (.frm)", "frm"));
			int returnVal = fc.showSaveDialog(AudioConversionUI.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String filePath = fc.getSelectedFile().getAbsolutePath();
				int extensionIndex = filePath.lastIndexOf('.');
				if(extensionIndex != -1)
					filePath = filePath.substring(0, extensionIndex);
				frameFilePath.setText(filePath + ".frm");
			} else {
				log.append("Save command cancelled by user.\n");
			}
		}
		if (e.getSource() == convertWavToFrameButton) {
			if(wavFilePath.getText().length() == 0) {
				log.append("No wav file selected for conversion.\n");
			} else if(frameFilePath.getText().length() == 0) {
				log.append("No output frame file specified.\n");
			} else {
				RawAudioFileStream rfs = null;
				try {
					rfs = new RawAudioFileStream(frameFilePath.getText(),true);
					WavUtil wavData = rfs.readWavFile(wavFilePath.getText());
					if(wavData != null){
						rfs.saveInFrameFormat(wavData);
						log.append("Conversion completed.\n");
					} else {
						log.append("Error in reading wav file.\n");
					}
				} catch(IOException ex) {
					ex.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					if(rfs !=null)
						rfs.close();
				}
				
			}
		}
	}
	
	public static void main(String[] args) {
		AudioConversionUI audioConversionUI = new AudioConversionUI();
		audioConversionUI.createAndShowGUI();
	}
}
