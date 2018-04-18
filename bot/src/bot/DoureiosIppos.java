package bot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import extractions.AbstractAction;
import extractions.AbstractionLayerAI;
import extractions.Harvest;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class DoureiosIppos extends AbstractionLayerAI {

	
	protected UnitTypeTable utt;
	UnitType workerType;
	UnitType baseType;
	UnitType barracksType;
	UnitType rangedType;
	UnitType lightType;
	UnitType heavyType;
	
	int maxBaseNum = 1;
	int maxBarracksNum = 1;
	
	int maxNumCollecters = 2;
	
	int defenderCountLimit = 2;
	
	int dirToEnemyBaseY = 0;
	int dirToEnemyBaseX = 0;
	
	public DoureiosIppos(UnitTypeTable a_utt)
	{
		this(a_utt, new AStarPathFinding());
	}
	
	public DoureiosIppos(UnitTypeTable a_utt ,PathFinding a_pf) {
		super(a_pf);
		reset(a_utt);
		// TODO Auto-generated constructor stub
	}

	public void reset()
	{
		super.reset();
	}
	
	public void reset(UnitTypeTable a_utt)
	{
		utt = a_utt;
		workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        rangedType = utt.getUnitType("Ranged");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
	}
	
	@Override
	public PlayerAction getAction(int playerID, GameState gs) throws Exception {
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(playerID);
		
		//List of units to define behaviours
		List<Unit> collecters = new LinkedList<Unit>();
		List<Unit> attackWorkers = new LinkedList<Unit>();
		List<Unit> bases = new LinkedList<Unit>();
		List<Unit> barracks = new LinkedList<Unit>();
		List<Unit> rangedDefenders = new LinkedList<Unit>();
		List<Unit> heavyDefenders = new LinkedList<Unit>();
		
		for(Unit u : pgs.getUnits())
		{
			if(u.getPlayer() == playerID)
			{
				if(u.getType() == workerType)
				{
					if(collecters.size() <= maxNumCollecters)
					{
						collecters.add(u);
					}else
					{attackWorkers.add(u);}
				}else if(u.getType() == baseType)
				{
					bases.add(u);
				}else if(u.getType() == barracksType)
				{
					barracks.add(u);
				}else if(u.getType() == lightType)
				{
					attackWorkers.add(u);
				}else if(u.getType() == rangedType)
				{
					rangedDefenders.add(u);
				}else if(u.getType() == heavyType)
				{
					heavyDefenders.add(u);
				}
			}
		}
		//Get the x and y direction to the enemy base.
		for(Unit u :pgs.getUnits())
		{
			if(u.getPlayer() >= 0 && u.getPlayer() != playerID)
			{
				if(bases.size() > 0)
				{
					if(bases.get(0) != null && u != null && u.getType() == baseType)
					{
						dirToEnemyBaseX = u.getX() - bases.get(0).getX();
						dirToEnemyBaseX = dirToEnemyBaseX / Math.abs(dirToEnemyBaseX);
						dirToEnemyBaseY = u.getY() - bases.get(0).getY();
						dirToEnemyBaseY = dirToEnemyBaseY / Math.abs(dirToEnemyBaseY);
					}
				}
			}
		}
		
		collecterBehaviour(collecters, p, pgs);
		attackClosest(attackWorkers, p, pgs);
		baseBehaviour(bases, p, pgs);
		barracksBehaviour(barracks, p, pgs);
		
		defendBaseLayer1(heavyDefenders, p, pgs);
		defendBaseLayer2(rangedDefenders,p,pgs);

		
		return translateActions(playerID,gs);
	}
	
	
	public void barracksBehaviour(List<Unit> barracks, Player p, PhysicalGameState pgs)
	{
		int rangedCount = 0;
		int heavyCount = 0;
		
		for(Unit u: pgs.getUnits())
		{
			if(u.getType() == rangedType && u.getPlayer() == p.getID())
			{
				rangedCount++;
			}else if(u.getType() == heavyType && u.getPlayer() == p.getID())
			{
				heavyCount++;
			}
		}
		
		//Needs to train heavy and ranged units to defend the base and light units which will flank the enemy.
		for(Unit u : barracks)
		{
			//Train heavy units which move forward 1 to defend the base. Train ranged units to stand behind the heavy units to also defend the base
			//Train light units to attack the enemy base.
			
			if(heavyCount <= defenderCountLimit)
			{
				
				if(p.getResources() >= heavyType.cost) {trainTowardEnemy(utt, u, heavyType, p.getID());}
				
			}else if(rangedCount < defenderCountLimit)
			{
				if(p.getResources() >= rangedType.cost) {trainTowardEnemy(utt, u, rangedType, p.getID());}
			}else if(p.getResources() >= lightType.cost)
			{
				train(u, lightType);
			}
		}
	}
	//Defensive techniques for the heavy units
	public void defendBaseLayer1(List<Unit> units, Player p, PhysicalGameState pgs)
	{
		Unit base = null;

		
		for(Unit u: pgs.getUnits())
		{
			if(u.getType() == baseType && u.getPlayer() == p.getID())
			{
				base = u;
			}
		}
		
		//Each unit should move to a position 1 away from the base.
		//They should then sit there and attack any units within range.
		int i = 0;
		for(Unit u : units)
		{
			if(base != null)
			{
				boolean yes = AttackIfInRange(4,u, GetClosestUnit(u, p, pgs));
				if(!yes)
				{
					if(i == 0) {move(u, base.getX(),  base.getY() + dirToEnemyBaseY * 2);}
					else if(i == 1) {move(u, base.getX() + dirToEnemyBaseX, base.getY() + dirToEnemyBaseY);}
					else if(i == 3) {move(u, base.getX() + dirToEnemyBaseX * 2, base.getY());}
				}
			}
			i++;
		}
	}
	
	public void defendBaseLayer2(List<Unit> units, Player p, PhysicalGameState pgs)
	{
		Unit base = null;

		
		for(Unit u: pgs.getUnits())
		{
			if(u.getType() == baseType && u.getPlayer() == p.getID())
			{
				base = u;
			}
		}
		
		int i = 0;
		for(Unit u : units)
		{
			boolean willAttack = AttackIfInRange(rangedType.attackRange, u, GetClosestUnit(u, p, pgs));
			if(!willAttack)
			{
				if(i == 0)
				{
					move(u,base.getX(), base.getY() + dirToEnemyBaseY);
				}else if(i == 1)
				{
					move(u,base.getX() + dirToEnemyBaseX, base.getY());
				}
			}
			
			i++;
		}
	}
	
	public void baseBehaviour (List<Unit> bases, Player p, PhysicalGameState pgs)
	{
		for(Unit u : bases)
		{
			if(p.getResources() >= workerType.cost)
			{
				train(u,workerType);
			}
		}
	}
	
	public void attackClosest(List<Unit> attackers, Player p, PhysicalGameState pgs)
	{
		for(Unit u : attackers)
		{
			Unit closestUnit = GetClosestUnit(u, p, pgs);
			
			if(closestUnit != null)
			{
				attack(u, closestUnit);
			}
		}
	}
	
	
	public void collecterBehaviour(List<Unit> collecters, Player p, PhysicalGameState pgs)
	{
		//Check there are units in the given list.
		if(collecters.isEmpty()) {return;}
		//Initalise variables
		int baseNum = 0;
		int barracksNum = 0;
		int basePosX = 0, basePosY = 0;
		
		List<Unit> freeWorkers = new LinkedList<Unit>();
		freeWorkers.addAll(collecters);
		
		//Count the number of friendly bases and barracks
		for(Unit u : pgs.getUnits())
		{
			if(u.getPlayer() == p.getID())
			{
				if(u.getType() == baseType) {baseNum++; basePosX = u.getX(); basePosY = u.getY();}
				else if(u.getType() == barracksType) {barracksNum++;}
			}
		}
		
		List<Integer> reservedPositions = new LinkedList<Integer>();
		//Build things if there are not as many as defined
		if(baseNum < maxBaseNum && !freeWorkers.isEmpty())
		{
			if(p.getResources() >= baseType.cost)
			{
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(),reservedPositions, p, pgs);
			}
		}
		if(barracksNum < maxBarracksNum && !freeWorkers.isEmpty())
		{
			if(p.getResources() >= barracksType.cost + 1)
			{
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, barracksType, basePosX + 2, basePosY,reservedPositions, p, pgs);
			}
		}
		
		//Free collecters not building things should be collecting and stockpiling resources.
		for(Unit u: freeWorkers)
		{
			Unit closestBase = null;
			Unit closestResource = null;
			
			int closestDistanceR = 0;
			int closestDistanceB = 0;
			
			for(Unit u2: pgs.getUnits())
			{
				if(u2.getType().isResource)
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if(d < closestDistanceR || closestResource == null)
					{
						closestResource = u2;
						closestDistanceR = d;
					}
				}else if(u2.getType().isStockpile && u2.getPlayer() == p.getID())
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if(d < closestDistanceB || closestBase == null)
					{
						closestDistanceB = d;
						closestBase = u2;
					}
				}
				
				if(closestResource != null && closestBase != null)
				{
					AbstractAction aa = getAbstractAction(u);
					if(aa instanceof Harvest)
					{
						Harvest h_aa = (Harvest)aa;
						if(h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {harvest(u, closestResource, closestBase);}
					}else
					{
						harvest(u, closestResource, closestBase);
					}
				}else {attack(u, GetClosestUnit(u, p, pgs));}
			}
		}
	}
	
	public boolean AttackIfInRange(int range,Unit u, Unit target)
	{
		boolean isInRange = false;
		int d = Math.abs(target.getX() - u.getX()) + Math.abs(target.getY() - u.getY());
		
		if(d <= range) {isInRange = true; attack(u, target);}
		return isInRange;
	}
	
	
	//Find the closest enemy unit to a friendly unit
	public Unit GetClosestUnit(Unit u, Player p, PhysicalGameState pgs)
	{
		Unit closestUnit = null;
		int closestDist = 0;
		
		
		for(Unit u2: pgs.getUnits())
		{
			if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
			{
				int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
				if(d < closestDist || closestUnit == null)
				{
					closestUnit = u2;
					closestDist = d;
				}
			}
		}
		return closestUnit;
	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return new DoureiosIppos(utt, pf);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		List<ParameterSpecification> parameters = new ArrayList<>();
		parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
		return null;
	}

}
