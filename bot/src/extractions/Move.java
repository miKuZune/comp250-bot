/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package extractions;

import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import rts.PhysicalGameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;
import util.XMLWriter;

/**
 *
 * @author santi
 */
public class Move extends AbstractAction {

    int x,y;
    PathFinding pf;

    
    public Move(Unit u, int a_x, int a_y, PathFinding a_pf) {
        super(u);
        x = a_x;
        y = a_y;
        pf = a_pf;
    }
    
    public boolean completed(GameState gs) {
        if (unit.getX()==x && unit.getY()==y) return true;
        return false;
    }
    
    
    public boolean equals(Object o)
    {
        if (!(o instanceof Move)) return false;
        Move a = (Move)o;
        if (x != a.x) return false;
        if (y != a.y) return false;
        if (pf.getClass() != a.pf.getClass()) return false;
        
        return true;
    }

    
    public void toxml(XMLWriter w)
    {
        w.tagWithAttributes("Move","unitID=\""+unit.getID()+"\" x=\""+x+"\" y=\""+y+"\" pathfinding=\""+pf.getClass().getSimpleName()+"\"");
        w.tag("/Move");
    }       

    public UnitAction execute(GameState gs, ResourceUsage ru) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        UnitAction move = pf.findPath(unit, x+y*pgs.getWidth(), gs, ru);
//        System.out.println("AStarAttak returns: " + move);
        if (move!=null && gs.isUnitActionAllowed(unit, move)) return move;
        return null;
    }
}
