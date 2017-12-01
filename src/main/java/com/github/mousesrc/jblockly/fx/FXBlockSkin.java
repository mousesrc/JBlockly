package com.github.mousesrc.jblockly.fx;

import java.util.List;

import com.github.mousesrc.jblockly.fx.FXBlockRow.Type;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.shape.SVGPath;

public class FXBlockSkin extends SkinBase<FXBlock> {

	private final SVGPath renderSVGPath = new SVGPath();

	private boolean performingLayout;

	private double[] tempArray;

	protected FXBlockSkin(FXBlock control) {
		super(control);

		init();
		initComponentsListener();
	}

	private void init() {
		getChildren().add(renderSVGPath);
	}

	private void initComponentsListener() {
		getChildren().addAll(getSkinnable().getFXRows());
		getFXRows().addListener(new ListChangeListener<Node>() {

			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends Node> c) {
				while (c.next()) {
					List<? extends Node> add = c.getAddedSubList();
					for (int i = 0, from = c.getFrom(), size = add.size(); i < size; i++)
						getChildren().add(i + from + 1, c.getAddedSubList().get(i));

					getChildren().removeAll(c.getRemoved());
				}
			}

		});
	}

	private ObservableList<FXBlockRow> getFXRows() {
		return getSkinnable().getFXRows();
	}

	private ConnectionType getConnectionType() {
		return getSkinnable().getConnectionType();
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		final double left = getConnectionType() == ConnectionType.LEFT ? FXBlockConstant.LEFT_WIDTH : 0;
		double width = 0;
		for (FXBlockRow row : getFXRows()) {
			double rowWidth = snapSize(row.prefWidth(-1));
			if (rowWidth > width)
				width = rowWidth;
		}
		return left + width;
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset,
			double leftInset) {
		double height = 0;
		for (FXBlockRow row : getFXRows()) {
			height += snapSize(row.prefHeight(-1));
		}
		return height;
	}

	@Override
	protected void layoutChildren(double contentX, double contentY, double contentWidth, double contentHeight) {
		if (performingLayout)
			return;
		performingLayout = true;
		
		double left = getConnectionType() == ConnectionType.LEFT ? FXBlockConstant.LEFT_WIDTH : 0;
		double top = 0;
		
		ObservableList<FXBlockRow> rows = getFXRows();
		
		render(rows);
		
		for (int i = 0, size = rows.size(); i < size; i++) {
			FXBlockRow row = rows.get(i);
			double width = snapSize(row.prefWidth(-1)), height = snapSize(row.prefHeight(-1));
			layoutInArea(row, left, top, width, height, -1, HPos.LEFT, VPos.TOP);
			top += height;
		}

		layoutInArea(renderSVGPath, 0, 0, renderSVGPath.prefWidth(-1), renderSVGPath.prefHeight(-1), -1, HPos.LEFT,
				VPos.TOP);

		performingLayout = false;
	}
	
	private void render(List<FXBlockRow> rows) {
		double[] rowComponentWidths = getRowRenderWidths(rows);
		int index = 0;
		double correntMaxWidth = 0;
		for (int i = 0, size = rows.size(); i < size; i++) {
			FXBlockRow row = rows.get(i);
			
			if (rowComponentWidths[i] > correntMaxWidth)
				correntMaxWidth = rowComponentWidths[i];

			if (row.getType() == Type.BRANCH) {
				alignRowRenderWidth(rows, index, i, correntMaxWidth);
				index = i + 1;
				correntMaxWidth = rowComponentWidths[i];
			}
		}

		alignRowRenderWidth(rows, index, rows.size() - 1, correntMaxWidth);
	}
	
	private void alignRowRenderWidth(List<FXBlockRow> rows, int from, int to, double alignedWidth) {
		for (int i = from; i <= to; i++)
			rows.get(i).setAlignedRenderWidth(alignedWidth);
	}
	
	private double[] getRowRenderWidths(List<FXBlockRow> rows) {
		double[] temp = getTempArray(rows.size());
		for (int i = 0, size = rows.size(); i < size; i++){
			FXBlockRow row = rows.get(i);
			temp[i] = row.getRenderWidth();
		}
		return temp;
	}
	
	private double[] getTempArray(int size) {
		if (tempArray == null) {
			tempArray = new double[size];
		} else if (tempArray.length < size) {
			tempArray = new double[Math.max(tempArray.length * 3, size)];
		}
		return tempArray;
	}
}
