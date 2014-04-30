package edu.byu.cs.roots.opg.chart.preset.templates;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import edu.byu.cs.roots.opg.chart.SpecificOptionsPanel;
import edu.byu.cs.roots.opg.gui.OnePageMainGui;

public class PresetChartOptionsPanel extends SpecificOptionsPanel {
	
	private static final long serialVersionUID = -6169055224201926981L;
	private JLabel jLabel = null;
	private JCheckBox roundedCheckBox = null;
	private JCheckBox borderCheckBox = null;
	private JCheckBox genLabelsCheckBox = null;
	private JCheckBox allowIntrusionCheckBox = null;
	private JCheckBox drawEndOfLineArrowsCheckBox = null;
	private JComboBox arrowStyleComboBox = null;
	NewPresetChartOptionsStylingPanel stylePanel = null;
	public PresetChartOptions options;
	
	/**
	 * Creates the advance option panel for the vertical chart menu 
	 * using the chart options we pass in.
	 * @param options - The options we want to include
	 * @param parent - The main user interface that we are going to 
	 * add the options too.
	 */
	public PresetChartOptionsPanel(PresetChartOptions options, OnePageMainGui parent){
		super();
		this.options = options;
		this.parent = parent;
		initialize();
		
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        jLabel = new JLabel();
        jLabel.setText("");
//        stylePanel = new PresetChartOptionsStyleEditPanel(this);
        stylePanel = new NewPresetChartOptionsStylingPanel(this);
        this.setPreferredSize(new Dimension(100, 100));
        this.setSize(new Dimension(201, 175));
        this.setBorder(BorderFactory.createTitledBorder(null, "Vertical Chart Options", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
        this.add(getRoundedCheckBox(), null);
        this.add(getBorderCheckBox(), null);
        this.add(getGenLabelsCheckBox(), null);
        this.add(getAllowIntrusionCheckBox(), null);
        this.add(getDrawEndOfLineArrowsCheckBox(), null);
        this.add(getArrowStyleComboBox(), null);
        this.add(jLabel, null);
        this.add(stylePanel);
			
	}
	
	public NewPresetChartOptionsStylingPanel getStyleEdit(){
		return stylePanel;
	}

	/**
	 * This method initializes roundedCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getRoundedCheckBox() {
		if (roundedCheckBox == null) {
			roundedCheckBox = new JCheckBox();
			roundedCheckBox.setText("Rounded Corners");
			roundedCheckBox.setSelected(options.isRoundedCorners());
			roundedCheckBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (!parent.maskEvents) {
						options.setRoundedCorners(roundedCheckBox.isSelected());
						parent.refresh();
					}
				}
			});
		}
		return roundedCheckBox;
	}
	
	/**
	 * This method initializes borderCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getBorderCheckBox() {
		if (borderCheckBox == null) {
			borderCheckBox = new JCheckBox();
			borderCheckBox.setText("Borders");
			borderCheckBox.setSelected(options.isBoxBorder());
			borderCheckBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (!parent.maskEvents) {
						options.setBoxBorder(borderCheckBox.isSelected());
						parent.refresh();
					}
				}
			});
		}
		return borderCheckBox;
	}

	/**
	 * This method initializes genLabelsCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getGenLabelsCheckBox() {
		if (genLabelsCheckBox == null) {
			genLabelsCheckBox = new JCheckBox();
			genLabelsCheckBox.setText("Generation Labels");
			genLabelsCheckBox.setSelected(options.isDrawTitles());
			genLabelsCheckBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (!parent.maskEvents) {
						options.setDrawTitles(genLabelsCheckBox.isSelected());
						parent.refresh();
					}
				}
			});
		}
		return genLabelsCheckBox;
	}
	
	/**
	 * This method initializes allowIntrusionCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getAllowIntrusionCheckBox() {
		if (allowIntrusionCheckBox == null) {
			allowIntrusionCheckBox = new JCheckBox();
			allowIntrusionCheckBox.setText("Allow Generation Intrusion");
			allowIntrusionCheckBox.setSelected(options.isAllowIntrusion());
			
			
			
			allowIntrusionCheckBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (!parent.maskEvents) {
						options.setAllowIntrusion(allowIntrusionCheckBox.isSelected());
						parent.refresh();
					}
				}
			});
		}
		return allowIntrusionCheckBox;
	}
	
	/**
	 * This method initializes drawEndOfLineArrowCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDrawEndOfLineArrowsCheckBox() {
		if (drawEndOfLineArrowsCheckBox == null) {
			drawEndOfLineArrowsCheckBox = new JCheckBox();
			drawEndOfLineArrowsCheckBox.setText("Draw End of Generation Arrows");
			drawEndOfLineArrowsCheckBox.setSelected(options.isDrawEndOfLineArrows());
			
			
			
			drawEndOfLineArrowsCheckBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (!parent.maskEvents) {
						options.setDrawEndOfLineArrows(drawEndOfLineArrowsCheckBox.isSelected());
						parent.refresh();
					}
				}
			});
		}
		return drawEndOfLineArrowsCheckBox;
	}
	
	private JComboBox getArrowStyleComboBox() {
		if (arrowStyleComboBox == null) {
			arrowStyleComboBox = new JComboBox();
			arrowStyleComboBox.addItem(PresetChartOptions.EndLineArrowStyle.GENERATIONS);
			arrowStyleComboBox.addItem(PresetChartOptions.EndLineArrowStyle.PARENTS);
			arrowStyleComboBox.addItem(PresetChartOptions.EndLineArrowStyle.SELF);			
			arrowStyleComboBox.setSelectedItem(options.getArrowStyle());
			
			arrowStyleComboBox.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (!parent.maskEvents) {
						options.setArrowStyle((PresetChartOptions.EndLineArrowStyle)arrowStyleComboBox.getSelectedItem());
						parent.refresh();
					}
				}
			});
		}
		return arrowStyleComboBox;
	}
	
	

}  //  @jve:decl-index=0:visual-constraint="10,10" 
