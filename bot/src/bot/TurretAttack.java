/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import rts.PhysicalGameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;
import util.XMLWriter;

public class TurretAttack extends AbstractAction  {
    Unit target;
    PathFinding pf;
    
    public TurretAttack(Unit u, Unit a_target, PathFinding a_pf) {
        super(u);
        target = a_target;
        pf = a_pf;
    }
    
    
    public boolean completed(GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        if (!pgs.getUnits().contains(target)) return true;
        return false;
    }
    
    
    public boolean equals(Object o)
    {
        if (!(o instanceof TurretAttack)) return false;
        TurretAttack a = (TurretAttack)o;
        if (target.getID() != a.target.getID()) return false;
        if (pf.getClass() != a.pf.getClass()) return false;
        
        return true;
    }

    
    public void toxml(XMLWriter w)
    {
        w.tagWithAttributes("Attack","unitID=\""+getUnit().getID()+"\" target=\""+target.getID()+"\" pathfinding=\""+pf.getClass().getSimpleName()+"\"");
        w.tag("/Attack");
    }
    

    public UnitAction execute(GameState gs, ResourceUsage ru) {
        
        int dx = target.getX()-getUnit().getX();
        int dy = target.getY()-getUnit().getY();
        double d = Math.sqrt(dx*dx+dy*dy);
        if (d<=getUnit().getAttackRange()) {
            return new UnitAction(UnitAction.TYPE_ATTACK_LOCATION,target.getX(),target.getY());
        } else {
            // move towards the unit:
    //        System.out.println("AStarAttak returns: " + move);
            UnitAction move = pf.findPathToPositionInRange(getUnit(), target.getX()+target.getY()*gs.getPhysicalGameState().getWidth(), getUnit().getAttackRange(), gs, ru);
            if (move!=null && gs.isUnitActionAllowed(getUnit(), move)) return move;
            return null;
        }        
    }    
}
