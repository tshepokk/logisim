/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.appear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

public class ClipboardActions extends Action {
	
	public static Action cut(AppearanceCanvas canvas) {
		return new ClipboardActions(true, canvas);
	}
	
	public static Action copy(AppearanceCanvas canvas) {
		return new ClipboardActions(false, canvas);
	}
	
	private boolean remove;
	private AppearanceCanvas canvas;
	private CanvasModel canvasModel;
	private List<CanvasObject> oldClipboard;
	private Map<CanvasObject, Integer> affected;
	private List<CanvasObject> newClipboard;

	private ClipboardActions(boolean remove, AppearanceCanvas canvas) {
		this.remove = remove;
		this.canvas = canvas;
		this.canvasModel = canvas.getModel();
		
		ArrayList<CanvasObject> newClip = new ArrayList<CanvasObject>();
		ArrayList<CanvasObject> aff = new ArrayList<CanvasObject>();
		for (CanvasObject o : canvas.getSelection().getSelected()) {
			if (o.canRemove()) {
				aff.add(o);
				newClip.add(o.clone());
			}
		}
		newClip.trimToSize();
		affected = ZOrder.getZIndex(aff, canvasModel);
		newClipboard = newClip;
	}
	
	@Override
	public String getName() {
		if (remove) {
			return Strings.get("cutSelectionAction");
		} else {
			return Strings.get("copySelectionAction");
		}
	}
	
	@Override
	public void doIt(Project proj) {
		oldClipboard = new ArrayList<CanvasObject>(Clipboard.get().getObjects());
		Clipboard.set(newClipboard);
		if (remove) {
			canvasModel.removeObjects(affected.keySet());
		}
	}
	
	@Override
	public void undo(Project proj) {
		if (remove) {
			canvasModel.addObjects(affected);
			canvas.getSelection().clearSelected();
			canvas.getSelection().setSelected(affected.keySet(), true);
		}
		Clipboard.set(oldClipboard);
	}
	
}
