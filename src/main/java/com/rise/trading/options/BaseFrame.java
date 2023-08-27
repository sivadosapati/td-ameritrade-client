package com.rise.trading.options;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFrame;

public abstract class BaseFrame extends JFrame {

	private String fileName = this.getClass().getName();

	public BaseFrame() {
		createComponents();
		addComponents();
		this.addWindowListener(new MyWindowListener());
		adjustSize();
		setVisible(true);
	}
	
	
	public abstract void createComponents();
	public abstract void addComponents();
	public void adjustSize() {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
			Point p = (Point) ois.readObject();
			Dimension d = (Dimension) ois.readObject();
			setLocation(p);
			setSize(d);
		} catch (Exception e) {
			setDefaultSize();
			e.printStackTrace();
		}
	}
	
	public void setDefaultSize() {
		setSize(1400, 900);
	}

	class MyWindowListener implements WindowListener {

		@Override
		public void windowOpened(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowClosing(WindowEvent e) {
			System.out.println("Closing window");
			JFrame frame = BaseFrame.this;
			Point p = frame.getLocation();
			Dimension d = frame.getSize();
			System.out.println(p + " -> " + d);
			try {
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName));
				oos.writeObject(p);
				oos.writeObject(d);
				oos.flush();
				oos.close();

			} catch (Exception ee) {
				ee.printStackTrace();
			}
			System.exit(0);

		}

		@Override
		public void windowClosed(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowIconified(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeiconified(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowActivated(WindowEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			// TODO Auto-generated method stub

		}

	}

}
