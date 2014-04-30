package edu.byu.cs.roots.opg.chart.landscape;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JPanel;
import edu.byu.cs.roots.opg.chart.ChartDrawInfo;
import edu.byu.cs.roots.opg.chart.ChartMaker;
import edu.byu.cs.roots.opg.chart.ChartMarginData;
import edu.byu.cs.roots.opg.chart.ChartOptions;
import edu.byu.cs.roots.opg.chart.ShapeInfo;
import edu.byu.cs.roots.opg.chart.cmds.DrawCmdMoveTo;
import edu.byu.cs.roots.opg.chart.cmds.DrawCmdPicture;
import edu.byu.cs.roots.opg.chart.cmds.DrawCmdRelLineTo;
import edu.byu.cs.roots.opg.chart.cmds.DrawCmdSetFont;
import edu.byu.cs.roots.opg.chart.cmds.DrawCmdText;
import edu.byu.cs.roots.opg.chart.preset.templates.ChartMargins;
import edu.byu.cs.roots.opg.chart.preset.templates.DescBoxParent;
import edu.byu.cs.roots.opg.chart.preset.templates.PresetChartOptions;
import edu.byu.cs.roots.opg.chart.preset.templates.StylingBoxScheme;
import edu.byu.cs.roots.opg.chart.preset.templates.PresetChartOptionsPanel;
import edu.byu.cs.roots.opg.fonts.OpgFont;
import edu.byu.cs.roots.opg.gui.OnePageMainGui;
import edu.byu.cs.roots.opg.model.Family;
import edu.byu.cs.roots.opg.model.Gender;
import edu.byu.cs.roots.opg.model.ImageFile;
import edu.byu.cs.roots.opg.model.Individual;
import edu.byu.cs.roots.opg.model.OpgSession;
import edu.byu.cs.roots.opg.util.NameAbbreviator;
import edu.byu.cs.roots.opg.model.OpgOptions;

public class LandscapeChartMaker implements ChartMaker {
	private static final long serialVersionUID = 1L;
	protected ChartDrawInfo chart = null;
	protected ChartMargins chartMargins = null;
	/**
	 * A list of AncesTree (The max in here is two, one for root, one for spouse)
	 */
	protected ArrayList<AncesTree> ancesTrees;
	protected double ancesMinHeight;
	protected double descMinHeight;
	protected boolean includeSpouses;
	protected boolean allowIntrusion;
	protected boolean drawTreeHasChanged = false;
	
	protected ArrayList<DescTree> descTrees;
	
	protected AncesBox ancesBox = null;
	protected DescBox descBox = null;
	protected LandscapeChartOptions ops;
	protected Individual root; //the root individual currently on the tree
	/**
	 * Amount of visible ancestor generations
	 */
	protected int ancesGens = -1;
	/**
	 * Amount of visible descendant generations
	 */
	protected int descGens = -1;
	
	/**
	 * A list of pre-determined box style variables
	 */
	StylingBoxScheme boxStyles;
	
	protected ArrayList<ArrayList<AncesBox>> ancesGenPositions;
	
	protected int[] maxSpouseLineOffset;
	protected float maxFont = 12, minFont=8;
	
	//chart margin size - consider moving to VerticalChartOptions
	
	double headerSize = 0;//size (in points) of title(s) at top of chart
	//TODO implement Titles
	double titleSize = 0;//size of chart title (to be implemented later)
	float labelFontSize = 12;//font size of generation labels at top of chart 
	
	protected ArrayList<ImageFile> images = new ArrayList<ImageFile>();
	
	private boolean isPrimaryMaker = false;
	
	public ChartOptions convertToSpecificOptions(ChartOptions options) {
		LandscapeChartOptions newOptions = new LandscapeChartOptions(options);
		
		//set default values for options specific to Vertical Chart here
		newOptions.setBoxBorder(true);
		newOptions.setRoundedCorners(true);
		newOptions.setDrawTitles(true);
		newOptions.setAllowIntrusion(false);
		newOptions.setDrawEndOfLineArrows(false);
		newOptions.setArrowStyle(PresetChartOptions.EndLineArrowStyle.GENERATIONS);
		newOptions.setPaperOrientationChoice(false);
		newOptions.setPaperWidthChoice(true);
		newOptions.setPaperHeightChoice(true);
		newOptions.setLandscape(true);
		newOptions.setIncludeSpouses(false);
		newOptions.setSpouseIncludedChoice(false);
		

		return newOptions;
	}
	
	public void convertOpgOptions(OpgOptions options){
		options.setChartMargins(72, 72, 72, 72);
		options.setPreferredLength(false, 0);
	}

	public ChartDrawInfo getChart(ChartOptions options, OpgSession session) {
		//  Account for margins and titles
		ops = (LandscapeChartOptions) options;
		
		//chart initialization and preparation section
		if (chart == null ||  root != ops.getRoot() || ancesGens != ops.getAncesGens() || descGens != ops.getDescGens()
				|| includeSpouses != ops.isIncludeSpouses() || ops.getDrawTreeHasChanged()
				|| allowIntrusion != ops.isAllowIntrusion() || ops.getStyleBoxChanged() || ops.getMarginsChanged()){
			
			initializeChart(session);
		}

		//chart generation - draw chart again to new paper size or options
		if (session.isChanged())
		{	
			generateChart(session);
		}
		
		return chart;
	}

	public JPanel getSpecificOptionsPanel(ChartOptions options, OnePageMainGui parent)
	{
		PresetChartOptions ops = (PresetChartOptions)options;
		return new PresetChartOptionsPanel(ops, parent);
	}

	protected void initializeChart(OpgSession session)
	{	
		OpgOptions opgOptions = session.getOpgOptions();
		chart = new ChartDrawInfo(0,0);
		//chartMargins = new ChartMargins(chart, marginSize);
		ChartMarginData margins = opgOptions.getChartMargins();
		chartMargins = new ChartMargins(chart, margins, headerSize);
		boolean includeSpouseChanged = includeSpouses != ops.isIncludeSpouses();
		boolean allowIntrusionChanged = allowIntrusion != ops.isAllowIntrusion();
		boolean styleBoxChanged = ops.getStyleBoxChanged();
		boolean drawTreeChanged = ops.getDrawTreeHasChanged();
		boolean newChartScheme = opgOptions.getNewChartScheme();
		
		allowIntrusion = ops.isAllowIntrusion();
		includeSpouses = ops.isIncludeSpouses();
		ops.setStyleBoxChanged(false);
		ops.setDrawTreeHasChanged(false);
		opgOptions.setNewChartScheme(false);
		ops.setMarginsChanged(false);
		
		
		
		
		//initialize box data structures (create tree)
		//This triggers if: 
		//Program started, changed the root,
		//Changed whether spouse is shown or not, or collapsed a tree
		if (root != ops.getRoot() || includeSpouseChanged  || drawTreeChanged)
		{	
			root = ops.getRoot();
			if (isPrimaryMaker)
				opgOptions.resetDuplicateMap();
			
			ancesGens = -1; //reset ancestor generations to allow recalculation of tree layout
			
			//reset isInTree flags for individuals & families
			//this is so we can rebuild the tree from scratch
			if (root == null)
				System.out.println("NULL ROOT!");
//			root.resetFlags();
//			//if the spouse is visible, their flags need to be reset as well
//			if (includeSpouses){
//				for (Family fam : root.fams){
//					Individual spouse = (root.gender == Gender.MALE)? fam.wife : fam.husband;
//					if (spouse != null)
//						spouse.resetFlags();
//				}
//			}
			
			session.resetIndiFlags();
			
			//set up list of AncesTree (The max in here is two, one for root, one for spouse)
			ancesTrees = new ArrayList<AncesTree>();
			
			//add root ancesTree
			ancesTrees.add(new AncesTree(root, session));
			
			//TODO This is where we can change auto resize when a tree is collapsed
			
			//Gets the max depth of the trees
			int maxOfMaxAncesGens = ancesTrees.get(0).ancesBox.maxGensInTree-1;/*ancesTrees.get(0).ancesBox.maxVisibleGensInTree;*///(ancesTrees.get(0).ancesBox.maxVisibleGensInTree == 0? ancesTrees.get(0).ancesBox.maxGensInTree:ancesTrees.get(0).ancesBox.maxVisibleGensInTree );
			
			//If the spouse goes further, updates max depth to theirs
			if (includeSpouses)
				for (Family fam : root.fams)
				{
					Individual spouse = (root.gender == Gender.MALE)? fam.wife : fam.husband;
					if (spouse != null)
					{
						ancesTrees.add(new AncesTree(spouse, session));
						int spouseMaxAncesGens = ancesTrees.get(ancesTrees.size()-1).ancesBox.maxGensInTree-1;/*ancesTrees.get(0).ancesBox.maxVisibleGensInTree;*///(ancesTrees.get(0).ancesBox.maxVisibleGensInTree == 0? ancesTrees.get(0).ancesBox.maxGensInTree:ancesTrees.get(0).ancesBox.maxVisibleGensInTree );
						if (spouseMaxAncesGens > maxOfMaxAncesGens)
							maxOfMaxAncesGens = spouseMaxAncesGens;
					}
				}
			
			//Makes sure the slider isn't allowing more generations then are possible using max depth
			if(isPrimaryMaker)
				opgOptions.setMaxAncesSlider(maxOfMaxAncesGens, isPrimaryMaker);
			if (ops.getAncesGens() < 5)
			{
				ops.setAncesGens(Math.min(maxOfMaxAncesGens, 5), session);
			}
			if(ops.getAncesGens() > opgOptions.getMaxAncesSlider())
				ops.setAncesGens(opgOptions.getMaxAncesSlider(), session);
			
			
			//set up DescTrees by resetting the flags so the tree can be rebuilt
			session.resetFamilyFlags();
			session.resetFamilyList();
			descTrees = new ArrayList<DescTree>();
			descTrees.add(new DescTree(root, session));
			int maxOfMaxDescGens = descTrees.get(0).descBox.maxGensInTree-1;
			ops.setDescGens(0, session);
			
			if (ops.getDescGens() < maxOfMaxDescGens)
			{
				if (opgOptions.getMaxDescSlider() < maxOfMaxDescGens)
					opgOptions.setMaxDescSlider(maxOfMaxDescGens, isPrimaryMaker);
				ops.setDescGens(maxOfMaxDescGens, session);
			}
			opgOptions.setMaxDescSlider(maxOfMaxDescGens, isPrimaryMaker);
			
			//reset isInTree flags for individuals & families again 
			//to allow subsequent charts to have all flags clear
//			root.resetFlags();
//			if (includeSpouses)
//				for (Family fam : root.fams)
//				{
//					Individual spouse = (root.gender == Gender.MALE)? fam.wife : fam.husband;
//					if (spouse != null)
//						spouse.resetFlags();
//				}
						
			session.resetIndiFlags();
			
			
						
		}
		
		if (root != ops.getRoot() || includeSpouseChanged  || drawTreeChanged ||
				ancesGens != ops.getAncesGens() || descGens != ops.getDescGens() || newChartScheme){
			//Set the box look variables
			if (!newChartScheme)
			{
				ArrayList<StylingBoxScheme> styles = StyleBoxFactory.getStyleList(ops.getAncesGens(), ops.getDescGens());
				opgOptions.setChartStyles(styles);
				boxStyles = styles.get(0);
				opgOptions.setRefreshSchemeList(true);
			}
			for(AncesTree tree : ancesTrees){
				tree.ancesBox.setBoxStyle(boxStyles);
			}
			for(DescTree tree : descTrees){
				tree.descBox.setBoxStyle(boxStyles);
			}
			
			styleBoxChanged = true;
			
		}
		
		
		
		
		//calculate needed size for generation labels
		findLabelFontSize();
		
		//reset flags
//		root.resetFlags();
//		if (includeSpouses)
//		for (Family fam : root.fams)
//		{
//			Individual spouse = (root.gender == Gender.MALE)? fam.wife : fam.husband;
//			if (spouse != null)
//				spouse.resetFlags();
//		}

		session.resetIndiFlags();
		//clear duplicate labels
		
		
		//calculate base coordinates
		if ((ancesGens != ops.getAncesGens() || drawTreeChanged || includeSpouseChanged || 
				styleBoxChanged || allowIntrusionChanged) && ancesTrees != null)
		{
			ops.resetDuplicateCount();
			opgOptions.resetDuplicateLabels();
			ancesGens = ops.getAncesGens();
			for (AncesTree tree: ancesTrees)
			{
				tree.ancesBox.calcCoords(ops, this, 0, boxStyles, session);
				//reset flags
//				root.resetFlags();
//				if (includeSpouses)
//				for (Family fam : root.fams)
//				{
//					Individual spouse = (root.gender == Gender.MALE)? fam.wife : fam.husband;
//					if (spouse != null)
//						spouse.resetFlags();
//				}
				session.resetIndiFlags();
				tree.ancesBox.minHeight = tree.ancesBox.upperSubTreeHeight - tree.ancesBox.lowerSubTreeHeight;
								
				//expand boxes to fill gaps
				tree.ancesBox.vPos = 0;
				tree.ancesBox.setRelativePositions(0,ops.getAncesGens());
				
				
			}
			
		}
		updateAncesHeight();
		if ((descGens != ops.getDescGens() || drawTreeChanged || 
				includeSpouseChanged || styleBoxChanged || allowIntrusionChanged) && descTrees != null)
		{
			descGens = ops.getDescGens();
			for (DescTree tree: descTrees)
			{
				tree.calcCoords(ops);
				tree.descBox.minHeight = tree.descBox.upperSubTreeHeight - tree.descBox.lowerSubTreeHeight;
			
				//expand boxes to fill gaps
				tree.descBox.vPos = 0;
				tree.descBox.setRelativePositions(0, ops.getDescGens());
				
				
			}
		}
		updateDescHeight();
		
		//set chart options for portrait orientation chart
		if (!ops.isLandscape())
		{
			double whiteSpace =  margins.getTop() + margins.getBottom() + headerSize;
			//increase chart height to at least the minimum calculated height if necessary
			double chartHeight = Math.max(ancesMinHeight, descMinHeight) + whiteSpace;
			
//			if ((ops.getPaperLength()) < chartHeight)
//			{
			ops.setPaperLength(chartHeight);
			ops.setPaperWidth(boxStyles.preferredWidth);
//			}
			ops.setMinPaperLength(chartHeight);
			//set max paper length
			//: change to a dynamically calculated value instead of this hard-coded one
			
		}
		else
		{
			double whiteSpace =  margins.getLeft() + margins.getRight() + headerSize;
					
			//increase chart height (paper width) to at least the minimum calculated height if necessary
			double chartHeight = Math.max(ancesMinHeight, descMinHeight) + whiteSpace;
//			if ((ops.getPaperLength()) < chartHeight)
//			{
			ops.setPaperLength(chartHeight);
			ops.setPaperWidth(boxStyles.preferredWidth);
//			}
			ops.setMinPaperLength(chartHeight);
		}
		
		//this line just makes sure that the changed variable is set so that the chart generation section will execute 
		ops.setPaperLength(ops.getPaperLength());
	
	}
	
	protected void updateAncesHeight()
	{
		ancesMinHeight = 0;
		for (AncesTree tree: ancesTrees)
		{
			ancesMinHeight += tree.ancesBox.upperSubTreeHeight - tree.ancesBox.lowerSubTreeHeight;
		}
	}
	
	protected void updateDescHeight()
	{
		descMinHeight = 0;
		if(ops.isIncludeSpouses() && ops.getDescGens() > 0){
			ArrayList<DescBoxParent> descs = descTrees.get(0).descBox.children;
			for(DescBoxParent box: descs)
				descMinHeight += box.upperSubTreeHeight - box.lowerSubTreeHeight;
		}
		else if(ops.getDescGens() == 0)
			descMinHeight = 0;
		else
			for (DescTree tree: descTrees)
			{
				descMinHeight += tree.descBox.upperSubTreeHeight - tree.descBox.lowerSubTreeHeight;
			}		
	}

	protected void generateChart(OpgSession session)
	{
		OpgOptions opgOptions = session.getOpgOptions();
		
		
		//width and height vary based on paper orientation
		double paperHeight = (ops.getPaperWidth().width);
		double paperWidth = ops.getPaperLength();
		ChartMarginData margins = opgOptions.getChartMargins();
		//calculate needed size for generation labels
		findLabelFontSize();
		
		//create new chart
		chart = new ChartDrawInfo((int)paperWidth, (int)paperHeight);
		chartMargins = new ChartMargins(chart, margins);
		
		
		
		paperHeight -= margins.getLeft() + margins.getRight() + headerSize;
		paperWidth -= margins.getTop() + margins.getBottom();
		
		updateDescHeight();
		updateAncesHeight();
		//double ancesHeight = ancesBox.upperSubTreeHeight - ancesBox.lowerSubTreeHeight;
		double ancesHeight = ancesMinHeight;
		double descHeight = descMinHeight;//descBox == null ? 0 : descBox.upperSubTreeHeight - descBox.lowerSubTreeHeight;
		//double chartHeight = Math.max(ancesHeight, descHeight);
		double ancesRootYPos = 0;
		double descRootYPos = 0;
		
		//This is to make sure the Offset from the center is equal
		if (session.getOptions().isIncludeSpouses())
		{
			boxStyles.getDescStyle(0).setOffset(boxStyles.getAncesStyle(0).rootBackOffset);
			boxStyles.getDescStyle(0).setWidth(boxStyles.getAncesStyle(0).getBoxWidth());
		}
		else if (session.getOptions().getDescGens() == 0){
			boxStyles.getDescStyle(0).setOffset(boxStyles.getAncesStyle(0).getRelativeOffset());
			boxStyles.getDescStyle(0).setWidth(boxStyles.getAncesStyle(0).getBoxWidth());
		}
		else
		{
			boxStyles.getAncesStyle(0).setOffset(boxStyles.getDescStyle(0).rootBackOffset);
			boxStyles.getAncesStyle(0).setWidth(boxStyles.getDescStyle(0).getBoxWidth());
		}
		
		
		double rootXPos = (boxStyles.getTotalDescHeight((ops.isIncludeSpouses() || descGens == 0)?1:0, descGens) + boxStyles.getTotalDescOffset(0, descGens-1));
		
		double scaler = 1;
		double dscaler = paperWidth / descHeight;

		//set height modifier so that the chart is the correct size
		if (ancesHeight >= descHeight && descTrees != null && ancesTrees != null)
		{
			scaler = paperWidth / ancesHeight;
			//double scaler = ancesBox.setHeight(paperHeight);
			for (AncesTree tree: ancesTrees)
				tree.ancesBox.setScaler(scaler < 1.0 ? 1.0 : scaler);
			for (DescTree tree: descTrees)
				if(ops.isIncludeSpouses() && ops.getDescGens() > 0)
					tree.descBox.setScaler(dscaler < 1.0 ? 1.0 : dscaler);
				else
					tree.descBox.setScaler(scaler < 1.0 ? 1.0 : scaler);
			//descBox.setScaler(scaler);
			
			ancesHeight *= scaler;
			//rootYPos = -(ancesBox.lowerSubTreeHeight*scaler);
			ancesRootYPos = ancesTrees.get(0).ancesBox.upperSubTreeHeight;
//			descRootYPos = -ancesTrees.get(ancesTrees.size()-1).ancesBox.lowerSubTreeHeight*scaler;//chart.getYExtent() / 2 - marginSize;
//			descRootYPos += descTrees.get(0).descBox.lowerSubTreeHeight*scaler;
			descRootYPos = ancesRootYPos;
			if(descTrees.get(0).descBox.upperSubTreeHeight > ancesRootYPos)
				descRootYPos = descTrees.get(0).descBox.upperSubTreeHeight;
			else if((-descTrees.get(0).descBox.lowerSubTreeHeight) > (-ancesTrees.get(0).ancesBox.lowerSubTreeHeight))
				descRootYPos -= ((-descTrees.get(0).descBox.lowerSubTreeHeight) - (-ancesTrees.get(0).ancesBox.lowerSubTreeHeight));
			
			//TODO check and make sure the connecting lines still match up
			
			//This can be ignored
			if(ops.isIncludeSpouses() && ops.getDescGens() > 0)
				descRootYPos=0;
		}
		else if (descTrees != null && ancesTrees != null)
		{
			scaler = paperWidth / descHeight;//descBox.setHeight(paperHeight);
			if(ops.isIncludeSpouses() && ops.getDescGens() > 0)
				scaler = paperWidth / ancesHeight;
			for (DescTree tree: descTrees)
			{
				if(ops.isIncludeSpouses() && ops.getDescGens() > 0)
					tree.descBox.setScaler(dscaler < 1.0 ? 1.0 : dscaler);
				else
					tree.descBox.setScaler(scaler < 1.0 ? 1.0 : scaler);
			}
			for (AncesTree tree: ancesTrees)
			{
				tree.ancesBox.setScaler(scaler < 1.0 ? 1.0 : scaler);
			}
			
			//ancesRootYPos = descRootYPos = -descTrees.get(0).descBox.lowerSubTreeHeight;
			descRootYPos = descTrees.get(0).descBox.upperSubTreeHeight;
			ancesRootYPos = descRootYPos;
			if(ancesTrees.get(0).ancesBox.upperSubTreeHeight > descRootYPos)
				ancesRootYPos = ancesTrees.get(0).ancesBox.upperSubTreeHeight;
			else if((-ancesTrees.get(0).ancesBox.lowerSubTreeHeight) > (-descTrees.get(0).descBox.lowerSubTreeHeight))
				ancesRootYPos -= (-ancesTrees.get(0).ancesBox.lowerSubTreeHeight) - (-descTrees.get(0).descBox.lowerSubTreeHeight);
			
			
			//TODO check and make sure the connecting line still match up			
			
			//ancesRootYPos = descTrees.get(descTrees.size()-1).descBox.upperSubTreeHeight*scaler;//chart.getYExtent() / 2 - marginSize;
			//ancesRootYPos += ancesTrees.get(ancesTrees.size()-1).ancesBox.upperSubTreeHeight*scaler;
			
			if(ops.isIncludeSpouses() && ops.getDescGens() > 0)
				ancesRootYPos=0;
		}
				
		//draw boxes on chart
		//ancesBox.drawAncesRootTree(chartMargins, ops, ancesGenPositions, 0, rootYPos);
		//descBox.drawDescRootTree(chartMargins, ops);
		double maxYPos=0,minYPos=paperHeight;
		if (ancesTrees != null)
		{
			for (int i = ancesTrees.size()-1; i >= 0; --i)
			{
				AncesTree tree = ancesTrees.get(i);
//				ancesRootYPos += (tree.ancesBox.upperSubTreeHeight*scaler);
				double rootYPos = (boxStyles.getTotalDescHeight(0, descGens) + boxStyles.getTotalDescOffset(0, descGens-1) - boxStyles.getTotalAncesHeight(0, 0));
				
				tree.DrawTree(chartMargins, ops, ancesRootYPos, rootYPos, session);
				//if include spouse and descendants we need to connect some lines.
				if(ops.isIncludeSpouses() && ops.getDescGens() > 0){
					if(ancesRootYPos > maxYPos)
						maxYPos = ancesRootYPos;
					if(ancesRootYPos < minYPos)
						minYPos = ancesRootYPos;
					double descXPos = boxStyles.getTotalDescWidth(2, descGens) + boxStyles.getTotalDescOffset(1, descGens - 1);
					double midDistance = (rootXPos - descXPos - boxStyles.getTotalDescWidth(1, 1))/2;
					chartMargins.addDrawCommand(new DrawCmdMoveTo(chartMargins.xOffset(rootXPos-midDistance),chartMargins.yOffset(50)));
					chartMargins.addDrawCommand(new DrawCmdRelLineTo(midDistance,0,1,Color.black));
				}
//					ancesRootYPos += tree.ancesBox.upperSubTreeHeight* scaler;
			}
		}
		
		if (descTrees != null)
		{
			//works differently if spouse is included.
			if(ops.isIncludeSpouses() && ops.getDescGens() > 0){
				descRootYPos=0;
				DescTree t = descTrees.get(descTrees.size()-1);
				ArrayList<DescBoxParent> children = t.descBox.children;
				double descXPos = rootXPos - boxStyles.getTotalDescHeight(1, 1)/2.0;
				double midDistance = (rootXPos - descXPos - boxStyles.getTotalDescHeight(1, 1))/2;
				
				//Draw each child
				for (int i = 0; i < children.size(); i++){
					DescBoxParent child = children.get(i);
					descRootYPos += -(child.lowerSubTreeHeight*dscaler);
					child.drawDescRootTree(chartMargins, ops, t.descGenPositions,descRootYPos , descXPos, session);
					chartMargins.addDrawCommand(new DrawCmdMoveTo(chartMargins.xOffset(descRootYPos),chartMargins.yOffset(descXPos+boxStyles.getTotalDescWidth(1, 1))));
					chartMargins.addDrawCommand(new DrawCmdRelLineTo(0,midDistance,1,Color.black));
					if(descRootYPos > maxYPos)
						maxYPos = descRootYPos;
					if(descRootYPos < minYPos)
						minYPos = descRootYPos;
					descRootYPos += child.upperSubTreeHeight*dscaler;
				}
				chartMargins.addDrawCommand(new DrawCmdMoveTo(chartMargins.xOffset(minYPos),chartMargins.yOffset(descXPos+boxStyles.getTotalDescHeight(1, 1)+midDistance)));
				chartMargins.addDrawCommand(new DrawCmdRelLineTo(maxYPos-minYPos,0,1,Color.black));
			}
			else
				for (int i = descTrees.size()-1; i >= 0; --i)
				{
					DescTree tree = descTrees.get(i);
					//descRootYPos += (tree.descBox.upperSubTreeHeight*scaler);
					tree.DrawTree(chartMargins, ops, descRootYPos, rootXPos, session);
					//descRootYPos -= tree.descBox.lowerSubTreeHeight*scaler;
				}
		}
		
		
		//draw root connection lines
		if (descGens == 0)
		{
			//if only 2 spouses and no descendants, draw line between the two boxes
			if (ancesTrees.size() == 2)
			{
				AncesBox rootBox = ancesTrees.get(0).ancesBox;
				AncesBox spouseBox = ancesTrees.get(1).ancesBox;
				chartMargins.addDrawCommand(new DrawCmdMoveTo(chartMargins.xOffset( ops.isAllowIntrusion() ? boxStyles.getTotalAncesWidth(0, 0) / 3.0: boxStyles.getTotalAncesWidth(0, 0) / 2.0),
															chartMargins.yOffset( -spouseBox.lowerSubTreeHeight*scaler + spouseBox.upperBoxBound) ));
				chartMargins.addDrawCommand(new DrawCmdRelLineTo(0,((spouseBox.upperSubTreeHeight - rootBox.lowerSubTreeHeight)*scaler) - spouseBox.upperBoxBound + rootBox.lowerBoxBound,1,Color.BLACK ));
			}
			
			//: for 3 or more spouses, draw a connecting line to the side
		}
		
		
		//draw titles on chart
		if (ops.drawTitles)
			drawTitles(session);
		
		//draw Logo on chart - branding
		drawLogo(session);
		
		//see findLabelFontSize for more instructions :)
		
		
		for (ImageFile f : images){
			chart.addDrawCommand(new DrawCmdMoveTo(f.x, f.y));
			chart.addDrawCommand(new DrawCmdPicture(f.width, f.height, f.getImage()));
		}
		//session.resetChanged();

	}
	
	protected void drawTitles(OpgSession session)
	{
		//choose font size for generation labels
		FontRenderContext frc = NameAbbreviator.frc;
		
		//draw each generation label
		Font font = OpgFont.getDefaultSansSerifFont(Font.BOLD, labelFontSize);
		LineMetrics lm = font.getLineMetrics("gjpqyjCAPSQJbdfhkl", frc);
		ChartMarginData margins = session.getOpgOptions().getChartMargins();
		double horizPos = margins.getLeft() - lm.getHeight();
		double vertPos = margins.getBottom();//ops.getPaperWidth().width;// - marginSize - headerSize + (2*lm.getLeading()) + lm.getDescent();
		//vertPos += -marginStorage.getTop() - headerSize + (2*lm.getLeading()) + lm.getDescent();
		//draw ancestor labels
		chart.addDrawCommand(new DrawCmdSetFont(font, Color.RED));
		for(int gen = -ops.getDescGens(); gen <= ops.getAncesGens(); ++gen)
		{
			double curBoxHeight;
			if (gen < 0)
				curBoxHeight = boxStyles.getDescStyle(-gen).boxHeight;
			else
				curBoxHeight = boxStyles.getAncesStyle(gen).boxHeight;
			
			double width = font.getStringBounds(getGenerationLabel(gen, curBoxHeight), frc).getWidth();
			//draw label centered above boxes for generation, and justified left for intruding boxes
			if ((gen < 0)?boxStyles.getDescStyle(-gen).isIntruding == true:boxStyles.getAncesStyle(gen).isIntruding == true)
				chart.addDrawCommand(new DrawCmdMoveTo(horizPos, vertPos));
			else
				chart.addDrawCommand(new DrawCmdMoveTo(horizPos, vertPos + (curBoxHeight - width)/2.0));
			chart.addDrawCommand(new DrawCmdText(getGenerationLabel(gen, curBoxHeight), 270));
			//draw spouse's name beneath root's name if root only has one spouse
			if (gen == 0 && ops.isIncludeSpouses() && root.fams.size() == 1)
			{
				Individual spouse = (root.gender == Gender.MALE)? root.fams.get(0).wife : root.fams.get(0).husband;
				//If spouse is equal to null, do nothing! 
				if (spouse != null) {	
					double spouseVertPos = vertPos;
					double spouseHorizPos = horizPos;
					spouseHorizPos += lm.getHeight();
					width = font.getStringBounds("and", frc).getWidth();
					chart.addDrawCommand(new DrawCmdMoveTo(spouseHorizPos, spouseVertPos + (curBoxHeight - width)/2.0));
					chart.addDrawCommand(new DrawCmdText("and", 270));
					spouseHorizPos += lm.getHeight();
					NameAbbreviator.nameFit(spouse.namePrefix.trim(), spouse.givenName.trim(), spouse.surname, spouse.nameSuffix, (float)curBoxHeight, font);
					String spouseName = NameAbbreviator.getName();
					width = font.getStringBounds(spouseName, frc).getWidth();	
					chart.addDrawCommand(new DrawCmdMoveTo(spouseHorizPos, spouseVertPos + (curBoxHeight - width)/2.0));
					chart.addDrawCommand(new DrawCmdText(spouseName, 270));
				}
			}
			vertPos += curBoxHeight + (gen<0?boxStyles.getDescStyle(-gen - 1).getRelativeOffset():boxStyles.getAncesStyle(gen).getRelativeOffset());
		}
		
	}
	
	protected void drawLogo(OpgSession session)
	{
		ChartMarginData margins = session.getOpgOptions().getChartMargins();
		chartMargins.addDrawCommand(new DrawCmdMoveTo(margins.getLeft()+chartMargins.getXExtent()-150, margins.getBottom()-12));
		chart.addDrawCommand(new DrawCmdSetFont(OpgFont.getDefaultSerifFont(Font.PLAIN, 12),Color.LIGHT_GRAY));
		chart.addDrawCommand(new DrawCmdText(programLogo));
	}
	
	//this returns an appropriate label for the generation - gen - 0 = self, 1 = parents, -1 = children, etc.
	protected String getGenerationLabel(int gen, double boxWidth)
	{
		switch (gen)
		{
		case 0:
			//return root.givenName + " " + root.middleName + " " + root.surname;//
			Font font = OpgFont.getDefaultSansSerifFont(Font.BOLD, labelFontSize);
			NameAbbreviator.nameFit(root.namePrefix.trim(), root.givenName.trim(), root.surname, root.nameSuffix, (float)boxWidth, font);
			return NameAbbreviator.getName();
			
		case 1:
			return "Parents";
		case 2:
			return "Grandparents";
		case 3:
			return "Great-Grandparents";
		case -1:
			return "Children";
		case -2:
			return "Grandchildren";
		case -3:
			return "Great-Grandchildren";
		default:
			if (gen > 0)
				return gen-2 + getOrdinalSuffix(gen-2) + " GGP";	
			else
				return (-gen)-2 + getOrdinalSuffix((-gen)-2) + " GGC"; 
		}
	}
	
	protected static String getOrdinalSuffix(int gen)
	{
		if (11 <= (gen%100) && 13 >= (gen%100))
			return "th";

		switch (gen%10)
		{
		case 1:
			return "st";
		case 2:
			return "nd";
		case 3:
			return "rd";
		default:
			return "th";
		}
	}
	
	protected void findLabelFontSize()
	{
		//find width of longest label
		FontRenderContext frc = NameAbbreviator.frc;
		float testFontSize = 72;
		labelFontSize = testFontSize;
		Font font = OpgFont.getDefaultSansSerifFont(Font.BOLD, testFontSize);
		headerSize = 72;
		//iterates through each generation label, finding the one that takes up the most space
		for(int gen = -ops.getDescGens(); gen <= ops.getAncesGens(); ++gen)
		{
			double curBoxWidth = gen < 0? boxStyles.getTotalDescHeight(-gen, -gen):boxStyles.getTotalAncesHeight(gen, gen);
			double width = font.getStringBounds(getGenerationLabel(gen, curBoxWidth), frc).getWidth();
			float newFontSize = (float)(testFontSize * (curBoxWidth / width));
			if (newFontSize < labelFontSize){
				labelFontSize = newFontSize;
			}
				
		}
		//set font size so that longest label barely fits over box
		//setBoxWidth();
		
		final float MAXLABELFONTSIZE = 80;
		if(labelFontSize > MAXLABELFONTSIZE)
			labelFontSize=MAXLABELFONTSIZE;
		LineMetrics lm = font.deriveFont(labelFontSize).getLineMetrics("gjpqyjCAPSQJbdfhkl", frc);
		
		if (ops.isDrawTitles())
			headerSize = titleSize + lm.getHeight() + lm.getLeading();
		else
			headerSize = titleSize;
		
		
	}
	
	
	/**
	 * Goes through all visible ancestors and descendants, returning a LinkedList of their ShapeInfo
	 * @param max visible ancestors, chosen by user
	 * @param max visible descendants, chosen by user
	 * @return LinkedList of ShapeInfo of all visible people
	 */
	public LinkedList<ShapeInfo> getChartShapes(int maxAnc, int maxDesc, OpgSession session) 
	{
		LinkedList<ShapeInfo> retVal = new LinkedList<ShapeInfo>();
		for (AncesTree tree: ancesTrees)
			retVal = tree.ancesBox.getBoxes(retVal, 0, maxAnc, session);
		for (DescTree tree: descTrees)
			retVal = tree.descBox.getBoxes(retVal, 0, maxDesc);
		return retVal;
	}

	/**
	 * Goes through all visible ancestors and descendants, checking if the passed in point intersects with them.
	 * If so, returns the ShapeInfo of that person.
	 * @param x coord of click
	 * @param y coord of click
	 * @param max visible ancestors chosen by user
	 * @param max visible descendants chosen by user
	 * @return the ShapeInfo of the clicked person. Null if no intersect.
	 */
	public ShapeInfo getIndiIntersect(double x, double y, int maxAnc, int maxDesc, OpgSession session) 
	{
		for (AncesTree tree: ancesTrees)
		{
			ShapeInfo retVal = tree.ancesBox.checkIntersect(x, y, 0, maxAnc, session);
			if (retVal != null)
				return retVal;
			
		}
		for (DescTree tree: descTrees)
		{
			ShapeInfo retVal = tree.descBox.checkIntersect(x, y, 0, maxDesc);
			if (retVal != null)
			{

				return retVal;
			}
//			if (retVal == null)
//			{
//				System.out.println("NULL BOX RETURNED? HUH?!");
//			}
		}
			
				
		return null;
	}
	
	public StylingBoxScheme getBoxStyles(){
		return boxStyles;
	}

	public void setChartStyle(StylingBoxScheme style) {
		boxStyles = (StylingBoxScheme) style;
		
	}
	public ArrayList<ImageFile> getImages(){
		return images;
	}


	@Override
	public void setIsPrimaryMaker(boolean set) {
		isPrimaryMaker = set;
		
	}
	

}


