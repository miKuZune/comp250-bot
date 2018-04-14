package extractions;

import ai.abstraction.pathfinding.PathFinding;
import rts.GameState;
import rts.PhysicalGameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;
import util.XMLWriter;

public class attackNoMove extends AbstractAction
{

	Unit target;
	PathFinding pf;
	
	public attackNoMove(Unit u, Unit a_target, PathFinding a_pf) {
		super(u);
		target = a_target;
		pf = a_pf;
	}

	@Override
	public boolean completed(GameState gs) {
		PhysicalGameState pgs = gs.getPhysicalGameState();
		if(!pgs.getUnits().contains(target)) {return true;}
		else {return false;}
	}

	@Override
	public void toxml(XMLWriter w) {
		w.tagWithAttributes("Attack","unitID=\""+unit.getID()+"\" target=\""+target.getID()+"\" pathfinding=\""+pf.getClass().getSimpleName()+"\"");
        w.tag("/Attack");
	}

	@Override
	public UnitAction execute(GameState pgs, ResourceUsage ru) {
		
		int dx = target.getX() - unit.getX();
		int dy = target.getY() - unit.getY();
		double d = Math.sqrt(dx*dx+dy*dy);
		if(d <= unit.getAttackRange())
		{
			return new UnitAction(UnitAction.TYPE_ATTACK_LOCATION, target.getX(), target.getY());
		}else if(d <= unit.getAttackRange() + 1)
		{
			UnitAction move = pf.findPathToPositionInRange(unit, target.getX()+target.getY()*pgs.getPhysicalGameState().getWidth(), unit.getAttackRange(), pgs, ru);
            if (move!=null && pgs.isUnitActionAllowed(unit, move)) return move;
		}
		else {return null;}
		return null;
	}

}
