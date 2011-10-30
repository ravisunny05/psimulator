package psimulator.userInterface.Editor.DrawPanel.Components;

import java.awt.Color;
import java.awt.GradientPaint;
import java.util.ArrayList;
import java.util.List;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import psimulator.userInterface.Editor.DrawPanel.ZoomManager;

/**
 *
 * @author Martin
 */
public class BundleOfCables extends AbstractComponent{

    private ZoomManager zoomManager;
    
    private AbstractHwComponent component1;
    private AbstractHwComponent component2;
    
    private List<Cable> cables;
    
    private static final int LINE_WIDTH = 2;
    
    Line2D line = new Line2D.Float();
    
    
    public BundleOfCables(AbstractHwComponent component1, AbstractHwComponent component2, ZoomManager zoomManager){
        cables = new ArrayList<Cable>();
        this.component1 = component1;
        this.component2 = component2;
        this.zoomManager = zoomManager;
    }
    
    public AbstractHwComponent getComponent1(){
        return component1;
    }
    
    public AbstractHwComponent getComponent2(){
        return component2;
    }
    
    public Cable getIntersectingCable(Rectangle r){
        //throw new UnsupportedOperationException("Not supported yet.");
        for(Cable c : cables){
            if(c.intersects(r)){
                return c;
            }
        }
        return null;
    }

    public List<Cable> getCables() {
        return cables;
    }
 
    public int getCablesCount(){
        return cables.size();
    }
    
    /**
     * adds cable to bundle
     * @param c 
     */
    public void addCable(Cable c){
        cables.add(c);
    }
    
    /**
     * remove cable from bundle
     * @param c 
     */
    public void removeCable(Cable c){
        cables.remove(c);
    }
     
    @Override
    public void paintComponent(Graphics g) {
        
        // two points of line in the middle
        int x1 = component1.getCenterLocation().x;
        int y1 = component1.getCenterLocation().y;
        int x2 = component2.getCenterLocation().x;
        int y2 = component2.getCenterLocation().y;
        
        
        double L = Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
        
        // count difference between cables that will be applied
        double maxDiffernce = (zoomManager.getIconSize()/1.5) / cables.size();
        double optimalDifference = 12.0 * zoomManager.getCurrentScale();
        double difference = Math.min(maxDiffernce, optimalDifference);
        
        // set offset to start with
        double offsetPixels = -(difference * (cables.size()-1) /2.0);
               
        // for all cables
        for(Cable c : cables){
            // count starting point
            int x1p = (int)(x1 + offsetPixels * (y2-y1) / L);
            int y1p = (int)(y1 + offsetPixels * (x1-x2) / L);
            
            // count finishing point
            int x2p = (int)(x2 + offsetPixels * (y2-y1) / L);
            int y2p = (int)(y2 + offsetPixels * (x1-x2) / L);
            
            // paint cable
            c.paintComponent(g, x1p, y1p, x2p, y2p);
            
            // change offset for next cable
            offsetPixels += difference;
        }
    }
    
    @Override
    public int getWidth() {
        //return Math.abs(getX1()-getX2());
        return LINE_WIDTH;
    }

    @Override
    public int getHeight() {
        //return Math.abs(getY1()-getY2());
        return LINE_WIDTH;
    }

    @Override
    public int getX() {
        if(getX1()<=getX2()){
            return getX1();
        }
        return getX2();
    }

    @Override
    public int getY() {
        if(getY1()<=getY2()){
            return getY1();
        }
        return getY2();
    }

    
    @Override
    public boolean intersects(Point p) {
        Rectangle r = new Rectangle(p);
        return intersects(r);
    }

    @Override
    public boolean intersects(Rectangle r) {
        line.setLine(getP1(), getP2());
        return line.intersects(r);
    }

    public int getX1() {
        return getComponent1().getCenterLocation().x;
    }

    public int getY1() {
        return getComponent1().getCenterLocation().y;
    }

    public Point2D getP1() {
        return getComponent1().getCenterLocation();
    }

    public int getX2() {
        return getComponent2().getCenterLocation().x;
    }

    public int getY2() {
        return getComponent2().getCenterLocation().y;
    }

    public Point2D getP2() {
        return getComponent2().getCenterLocation();
    }
}
