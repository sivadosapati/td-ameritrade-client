package com.rise.trading.options;

import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ObjectEditingComponent extends JPanel {

	private Class clz;
	// private Object object;
	private Map<Field, JTextField> inputs = new LinkedHashMap<Field, JTextField>();

	public ObjectEditingComponent(Class cls) {
		this.clz = cls;
		create();
		addComponents();
	}

	public ObjectEditingComponent(Object object) {
		this(object.getClass());
		initialize(object);
	}

	private void initialize(Object object) {

		try {
			for (Map.Entry<Field, JTextField> entry : inputs.entrySet()) {
				entry.getValue().setText(entry.getKey().get(object) + "");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Object getObject() {
		try {
			Object object = clz.newInstance();
			for (Map.Entry<Field, JTextField> entry : inputs.entrySet()) {
				String value = entry.getValue().getText();
				entry.getKey().set(object, Double.parseDouble(value));
			}
			return object;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void addComponents() {
		setLayout(new GridLayout(inputs.size(), 2, 5, 5));
		for (Map.Entry<Field, JTextField> entry : inputs.entrySet()) {
			//JPanel p = new JPanel();
			add(new JLabel(entry.getKey().getName()+ " : ", JLabel.RIGHT));
			add(entry.getValue());
			//this.add(p);

		}

	}

	private void create() {
		Field fld[] = clz.getDeclaredFields();
		for (Field f : fld) {
			JTextField text = new JTextField(10);
			inputs.put(f, text);

		}
	}

}
