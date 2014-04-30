package edu.byu.cs.roots.opg.chart.landscape;

import edu.byu.cs.roots.opg.chart.ChartOptions;
import edu.byu.cs.roots.opg.chart.ChartType;
import edu.byu.cs.roots.opg.chart.preset.templates.PresetChartOptions;
import edu.byu.cs.roots.opg.model.PaperWidth;

/**
 * This class extends the ChartOptions class and adds the options
 * of getting whether we have a boarder around the boxes, if we
 * need to draw the titles, and if we should have rounded corners.
 * @author derek
 * @see ChartOptions
 * @version 1.0.0
 */
public class LandscapeChartOptions extends PresetChartOptions
{
	/** Serial ID for when we want to save this class as a bit stream. */
	private static final long serialVersionUID = -6787151923234766638L;
	
	/**
	 * Creates the Vertical Chart's Options by passing the options
	 * to the parent class and setting the chart type to 
	 * ChartType.VERTICAL
	 * @param options - The current options of this chart.
	 * @see ChartOptions
	 * @see ChartType
	 */
	public LandscapeChartOptions(ChartOptions options)
	{
		super(options);

		setPaperWidth(PaperWidth.findClosestFit(options.getPaperWidth().width));
		if(firstLoad){
			setPaperWidth(PaperWidth.findClosestFit(36*72));
			setPaperLength(48*72);
			firstLoad = false;
		}
	}
		
	
	
}
