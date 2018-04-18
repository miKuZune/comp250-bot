package extractions;

import rts.GameState;
import rts.PhysicalGameState;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import util.XMLWriter;

public class TrainTowardEnemy extends AbstractAction {

	protected UnitTypeTable utt;
	UnitType type;
	int PlayerID = 0;
	boolean completed = false;
	
	public TrainTowardEnemy(UnitTypeTable tt,Unit u, UnitType uType, int playerID) {
		super(u);
		type = uType;
		PlayerID = playerID;
		utt = tt;
	}

	@Override
	public boolean completed(GameState pgs) {
		return completed;
	}

	@Override
	public void toxml(XMLWriter w) {
		w.tagWithAttributes("Train","unitID=\""+unit.getID()+"\" type=\""+type.name+"\"");
        w.tag("/Train");
	}

	@Override
	public UnitAction execute(GameState gs, ResourceUsage ru) {
		PhysicalGameState pgs = gs.getPhysicalGameState();
		int x = unit.getX();
		int y = unit.getY();
		
		int trainDirection = -1;
		
		Unit ourBase = GetOurBase(pgs);
		Unit theirBase = GetEnemyBase(pgs);
		//Find the x and y deltas
		if(ourBase == null || theirBase == null)
		{
			return null;
		}
		int dirToEnemyBaseX = theirBase.getX() - ourBase.getX();
		int dirToEnemyBaseY = theirBase.getY() - ourBase.getY();
		//Normalise the x and y deltas (map them to a value of either 1 or -1)
		//Uses Math.abs in case of negative numbers so the result is still negative
		dirToEnemyBaseX = dirToEnemyBaseX / Math.abs(dirToEnemyBaseX);
		dirToEnemyBaseY = dirToEnemyBaseY / Math.abs(dirToEnemyBaseY);
		//Decide which UnitAction.Direction the found values are.
		//Up = -1, down = 1
		//Right = 1, left = -1
		
		int xDir = 0, yDir = 0;
		if(dirToEnemyBaseX < 0) {xDir = UnitAction.DIRECTION_LEFT;}
		else {xDir = UnitAction.DIRECTION_RIGHT;}
		
		if(dirToEnemyBaseY < 0) {yDir =  UnitAction.DIRECTION_UP;}
		else {yDir = UnitAction.DIRECTION_DOWN;}
		/*
		
		
		
		*/
		if (x < pgs.getWidth() - 1 && gs.free(x + dirToEnemyBaseX,y))
		{
			trainDirection = xDir;
		}else if(y > 0 && gs.free(x, y + dirToEnemyBaseY))
		{
			trainDirection = yDir;
		}
		
		
		
		completed = true;
		
		if(trainDirection != -1)
		{
			UnitAction ua = new UnitAction(UnitAction.TYPE_PRODUCE, trainDirection, type);
			if(gs.isUnitActionAllowed(unit, ua)) {return ua;}
		}
		
		return null;
	}

	
	public Unit GetOurBase(PhysicalGameState pgs)
	{
		Unit ourBase = null;
		
		for(Unit u: pgs.getUnits())
		{
			if(u.getPlayer() == PlayerID)
			{
				if(u.getType() == utt.getUnitType("Base"))
				{
					ourBase = u;
				}
			}
		}
		return ourBase;
	}
	
	public Unit GetEnemyBase(PhysicalGameState pgs)
	{
		Unit enemyBase = null;
		for(Unit u: pgs.getUnits())
		{
			if(u.getPlayer() >= 0 && u.getPlayer() != PlayerID)
			{
				if(u.getType() == utt.getUnitType("Base"))
				{
					enemyBase = u;
				}
			}
		}
		
		return enemyBase;
	}
}
