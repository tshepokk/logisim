package com.cburch.logisim.circuit.appear;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;

class AppearanceOrigin extends AppearanceElement {
	static final Attribute<Direction> FACING
		= Attributes.forDirection("facing", Strings.getter("appearanceFacingAttr"));
	static final List<Attribute<?>> ATTRIBUTES
		= UnmodifiableList.create(new Attribute<?>[] { FACING });
	
	private static final int RADIUS = 2;
	private static final int INDICATOR_LENGTH = 8;
	private static final Color SYMBOL_COLOR = new Color(0, 128, 0);
	
	private Direction facing;
	
	public AppearanceOrigin(Location location) {
		super(location);
		facing = Direction.EAST;
	}

	@Override
	public String getDisplayName() {
		return Strings.get("circuitOrigin");
	}
	
	@Override
	public Element toSvgElement(Document doc) {
		Location loc = getLocation();
		Element ret = doc.createElement("circ-origin");
		ret.setAttribute("x", "" + (loc.getX() - RADIUS));
		ret.setAttribute("y", "" + (loc.getY() - RADIUS));
		ret.setAttribute("width", "" + 2 * RADIUS);
		ret.setAttribute("height", "" + 2 * RADIUS);
		ret.setAttribute("facing", facing.toString());
		return ret;
	}

	public Direction getFacing() {
		return facing;
	}
	
	@Override
	public List<Attribute<?>> getAttributes() {
		return ATTRIBUTES;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <V> V getValue(Attribute<V> attr) {
		if (attr == FACING) {
			return (V) facing;
		} else {
			return super.getValue(attr);
		}
	}
	
	@Override
	protected void updateValue(Attribute<?> attr, Object value) {
		if (attr == FACING) {
			facing = (Direction) value;
		} else {
			super.updateValue(attr, value);
		}
	}
	
	@Override
	protected int getRadius() {
		return RADIUS;
	}

	@Override
	public void paint(Graphics g, Location handle, int handleDx, int handleDy) {
		Location location = getLocation();
		int x = location.getX();
		int y = location.getY();
		g.setColor(SYMBOL_COLOR);
		g.drawOval(x - RADIUS, y - RADIUS, 2 * RADIUS, 2 * RADIUS);
		Location e0 = location.translate(facing, RADIUS);
		Location e1 = location.translate(facing, RADIUS + INDICATOR_LENGTH);
		g.drawLine(e0.getX(), e0.getY(), e1.getX(), e1.getY());
	}
	
	@Override
	public Bounds getBounds() {
		Bounds bds = super.getBounds();
		Location center = getLocation();
		Location end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
		return bds.add(end);
	}

	@Override
	public boolean contains(Location loc) {
		if (super.contains(loc)) {
			return true;
		} else {
			Location center = getLocation();
			Location end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
			if (facing == Direction.EAST || facing == Direction.WEST) {
				return Math.abs(loc.getY() - center.getY()) < 2
					&& (loc.getX() < center.getX()) != (loc.getX() < end.getX());
			} else {
				return Math.abs(loc.getX() - center.getX()) < 2
					&& (loc.getY() < center.getY()) != (loc.getY() < end.getY());
			}
		}
	}
	
	@Override
	public List<Location> getHandles(Location handle, int dx, int dy) {
		Location center = getLocation();
		return UnmodifiableList.create(new Location[] {
				center, center.translate(facing, RADIUS + INDICATOR_LENGTH) });
	}
}
