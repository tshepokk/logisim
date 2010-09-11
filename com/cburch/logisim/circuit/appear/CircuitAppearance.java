package com.cburch.logisim.circuit.appear;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.draw.canvas.CanvasModelEvent;
import com.cburch.draw.canvas.CanvasModelListener;
import com.cburch.draw.canvas.CanvasObject;
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.model.Drawing;
import com.cburch.draw.model.DrawingMember;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class CircuitAppearance extends Drawing {
	private class MyListener implements CanvasModelListener {
		public void modelChanged(CanvasModelEvent event) {
			if (!settingDefault) {
				configureDefault(false);
				fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
			}
		}
	}
	
	private Circuit circuit;
	private EventSourceWeakSupport<CircuitAppearanceListener> listeners;
	private PortManager portManager;
	private CircuitPins circuitPins;
	private MyListener myListener;
	private boolean isDefault;
	private boolean settingDefault;
	
	public CircuitAppearance(Circuit circuit) {
		this.circuit = circuit;
		listeners = new EventSourceWeakSupport<CircuitAppearanceListener>();
		portManager = new PortManager(this);
		circuitPins = new CircuitPins(portManager);
		myListener = new MyListener();
		settingDefault = false;
		addCanvasModelListener(myListener);
		configureDefault(true);
	}
	
	public CircuitPins getCircuitPins() {
		return circuitPins;
	}
	
	public void addCircuitAppearanceListener(CircuitAppearanceListener l) {
		listeners.add(l);
	}
	
	public void removeCircuitAppearanceListener(CircuitAppearanceListener l) {
		listeners.remove(l);
	}
	
	void fireCircuitAppearanceChanged(int affected) {
		CircuitAppearanceEvent event;
		event = new CircuitAppearanceEvent(circuit, affected);
		for (CircuitAppearanceListener listener : listeners) {
			listener.circuitAppearanceChanged(event);
		}
	}
	
	public boolean isDefaultAppearance() {
		return isDefault;
	}
	
	void resetDefaultAppearance() {
		if (isDefault) {
			Collection<CanvasObject> shapes;
			shapes = DefaultAppearance.build(circuitPins.getPins());
			try {
				settingDefault = true;
				super.removeObjects(new ArrayList<CanvasObject>(getObjects()));
				super.addObjects(shapes);
			} finally {
				settingDefault = false;
			}
			fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
		}
	}
	
	void recomputePorts() {
		if (isDefault) {
			resetDefaultAppearance();
		} else {
			fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
		}
	}
	
	private void configureDefault(boolean value) {
		if (isDefault != value) {
			isDefault = value;
			if (value) {
				resetDefaultAppearance();
			}
		}
	}
	
	public Direction getFacing() {
		AppearanceOrigin origin = findOrigin();
		if (origin == null) {
			return Direction.EAST;
		} else {
			return origin.getFacing();
		}
	}
	
	public void setObjects(Collection<DrawingMember> members) {
		super.removeObjects(new ArrayList<CanvasObject>(super.getObjects()));
		super.addObjects(members);
		configureDefault(false);
		recomputePorts();
	}

	public void paintSubcircuit(Graphics g, Direction facing) {
		Direction defaultFacing = getFacing();
		double rotate = 0.0;
		if (facing != defaultFacing && g instanceof Graphics2D) {
			rotate = defaultFacing.toRadians() - facing.toRadians();
			((Graphics2D) g).rotate(rotate);
		}
		Location offset = findOriginLocation();
		g.translate(-offset.getX(), -offset.getY());
		for (CanvasObject shape : getObjects()) {
			if (!(shape instanceof AppearanceElement)) {
				Graphics dup = g.create();
				shape.paint(dup, null, 0, 0);
				dup.dispose();
			}
		}
		g.translate(offset.getX(), offset.getY());
		if (rotate != 0.0) {
			((Graphics2D) g).rotate(-rotate);
		}
	}
	
	private Location findOriginLocation() {
		AppearanceOrigin origin = findOrigin();
		if (origin == null) {
			return Location.create(100, 100);
		} else {
			return origin.getLocation();
		}
	}
	
	private AppearanceOrigin findOrigin() {
		for (CanvasObject shape : getObjects()) {
			if (shape instanceof AppearanceOrigin) {
				return (AppearanceOrigin) shape;
			}
		}
		return null;
	}

	@Override
	public void paint(Graphics g, Selection selection) {
		Set<CanvasObject> suppressed = selection.getDrawsSuppressed();
		List<CanvasObject> ports = new ArrayList<CanvasObject>();
		CanvasObject origin = null;
		for (CanvasObject shape : getObjects()) {
			if (shape instanceof AppearanceElement) {
				if (shape instanceof AppearancePort) {
					ports.add(shape);
				} else if (origin == null) {
					origin = shape;
				}
			} else {
				drawShape(g, shape, selection, suppressed);
			}
		}
		for (CanvasObject shape : ports) {
			drawShape(g, shape, selection, suppressed);
		}
		if (origin != null) {
			drawShape(g, origin, selection, suppressed);
		}
	}
	
	private void drawShape(Graphics g, CanvasObject shape, Selection selection,
			Set<CanvasObject> suppressed) {
		Graphics dup = g.create();
		if (suppressed.contains(shape)) {
			selection.drawSuppressed(dup, shape);
		} else {
			shape.paint(dup, null, 0, 0);
		}
		dup.dispose();
	}
	
	public Bounds getOffsetBounds() {
		return getBounds(true);
	}
	
	public Bounds getAbsoluteBounds() {
		return getBounds(false); 
	}
	
	private Bounds getBounds(boolean relativeToOrigin) {
		Bounds ret = null;
		Location offset = null;
		for (CanvasObject o : getObjects()) {
			if (o instanceof AppearanceElement) {
				Location loc = ((AppearanceElement) o).getLocation();
				if (o instanceof AppearanceOrigin) {
					offset = loc;
				}
				if (ret == null) {
					ret = Bounds.create(loc);
				} else {
					ret = ret.add(loc);
				}
			} else {
				if (ret == null) {
					ret = o.getBounds();
				} else {
					ret = ret.add(o.getBounds());
				}
			}
		}
		if (ret == null) {
			return Bounds.EMPTY_BOUNDS; 
		} else if (relativeToOrigin && offset != null) {
			return ret.translate(-offset.getX(), -offset.getY());
		} else {
			return ret;
		}
	}
	
	public SortedMap<Location, Instance> getPortOffsets(Direction facing) {
		Location origin = null;
		Direction defaultFacing = Direction.EAST;
		List<AppearancePort> ports = new ArrayList<AppearancePort>();
		for (CanvasObject shape : getObjects()) {
			if (shape instanceof AppearancePort) {
				ports.add((AppearancePort) shape);
			} else if (shape instanceof AppearanceOrigin) {
				AppearanceOrigin o = (AppearanceOrigin) shape;
				origin = o.getLocation();
				defaultFacing = o.getFacing();
			}
		}

		SortedMap<Location, Instance> ret = new TreeMap<Location, Instance>();
		for (AppearancePort port : ports) {
			Location loc = port.getLocation();
			if (origin != null) {
				loc = loc.translate(-origin.getX(), -origin.getY());
			}
			if (facing != defaultFacing) {
				loc = loc.rotate(defaultFacing, facing, 0, 0);
			}
			ret.put(loc, port.getPin());
		}
		return ret;
	}
	
	@Override
	public void addObjects(Collection<? extends CanvasObject> shapes) {
		super.addObjects(shapes);
		checkToFirePortsChanged(shapes);
	}
	
	@Override
	public void removeObjects(Collection<? extends CanvasObject> shapes) {
		super.removeObjects(shapes);
		checkToFirePortsChanged(shapes);
	}
	
	@Override
	public void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy) {
		super.translateObjects(shapes, dx, dy);
		checkToFirePortsChanged(shapes);
	}
	
	private void checkToFirePortsChanged(Collection<? extends CanvasObject> shapes) {
		if (affectsPorts(shapes)) {
			recomputePorts();
		}
	}
	
	private boolean affectsPorts(Collection<? extends CanvasObject> shapes) {
		for (CanvasObject o : shapes) {
			if (o instanceof AppearanceElement) {
				return true;
			}
		}
		return false;
	}
}
