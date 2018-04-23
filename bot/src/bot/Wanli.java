package bot;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import extractions.AbstractAction;
import extractions.AbstractionLayerAI;
import extractions.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public class Wanli extends AbstractionLayerAI
{
	//Variables
	protected UnitTypeTable utt;
	UnitType workerType;
	UnitType baseType;
	UnitType barracksType;
	UnitType rangedType;
	UnitType lightType;
	UnitType heavyType;
	
	boolean wallIsMoving = false;
	int currWallOffset = 0;
	
	int maxNumberCollecterWorkers = 2;
	int maxNumberAttackWorkers = 4;
	
	int tileCount = 0;
	
	public Wanli(UnitTypeTable a_utt)
	{
		this(a_utt, new AStarPathFinding());
	}
	
	public Wanli(UnitTypeTable a_utt,PathFinding a_pf) {
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
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		// Variables		
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
		
		tileCount = pgs.getWidth() * pgs.getHeight();
		
		
		if(tileCount <= 100)
		{
			workerRushActions(p, pgs);
		}else
		{
			flankActions(p, pgs);
		}
		
		
		return translateActions(player,gs);
	}
  
	
	public void workerRushActions(Player p, PhysicalGameState pgs)
	{
		List<Unit> collecter = new LinkedList<Unit>();
		List<Unit> rushAttackers = new LinkedList<Unit>();
		List<Unit> bases = new LinkedList<Unit>();
		List<Unit> barracks = new LinkedList<Unit>();
		
		for(Unit u : pgs.getUnits())
		{
			if(u.getPlayer() == p.getID())
			{
				if(u.getType() == workerType)
				{
					if(collecter.size() < maxNumberCollecterWorkers)
					{
						collecter.add(u);
					}else
					{
						rushAttackers.add(u);
					}
				}else if(u.getType() == baseType)
				{
					bases.add(u);
				}else if(u.getType() == barracksType)
				{
					barracks.add(u);
				}
				else if(u.getType() == heavyType)
				{
					rushAttackers.add(u);
				}
			}
		}
		
		workerBehaviour(collecter, p, pgs);
		attackClosest(rushAttackers, p, pgs);
		baseBehaviour(bases, p, pgs);
		barrackBehaviour(barracks, p , pgs);
	}
	
	public void flankActions(Player p, PhysicalGameState pgs)
	{
		//Unit lists
				List<Unit> collecters = new LinkedList<Unit>();
				List<Unit> attackerWorkers = new LinkedList<Unit>();
				List<Unit> bases = new LinkedList<Unit>();
				List<Unit> barracks = new LinkedList<Unit>();
				List<Unit> sideAttackers = new LinkedList<Unit>();
				
				//Give units behaviours.
				for(Unit u : pgs.getUnits())
				{
					if(u.getPlayer() == p.getID())
					{
						if(u.getType() == workerType)
						{
							if(collecters.size() <= maxNumberCollecterWorkers)
							{
								collecters.add(u);
							}else
							{
								attackerWorkers.add(u);
							}	
						}
						
						else if(u.getType() == baseType){bases.add(u); currWallOffset = u.getY();}
						else if(u.getType() == barracksType){barracks.add(u);}
						else if(u.getType() == rangedType) {sideAttackers.add(u);}
						else if(u.getType() == lightType) {sideAttackers.add(u);}
					}
				}
				
				
				workerBehaviour(collecters, p, pgs);
				attackClosest(attackerWorkers, p, pgs);
				baseBehaviour(bases, p, pgs);
				barrackBehaviour(barracks, p, pgs);
				flankAttack(sideAttackers, p, pgs);
				//wallBehaviour(rangedTypes, p, pgs);
				
	}
	
	public void baseBehaviour(List<Unit> bases, Player p, PhysicalGameState pgs)
	{
		if(bases.isEmpty()) {return;}
		
		int numOfWorkers = 0;
		
		for(Unit u : pgs.getUnits())
		{
			if(u.getPlayer() == p.getID() && u.getType() == workerType) {numOfWorkers++;}
		}
		
		if(p.getResources() >= workerType.cost && numOfWorkers <= maxNumberCollecterWorkers + maxNumberAttackWorkers)
		{
			trainTowardEnemy(utt, bases.get(0), workerType, p.getID());
		}
	}
	
	public void barrackBehaviour(List<Unit> barracks, Player p, PhysicalGameState pgs)
	{
		int lightCount = 0;
		int rangedCount = 0;
		
		for(Unit u : pgs.getUnits())
		{
			if(u.getPlayer() == p.getID())
			{
				if(u.getType() == lightType) {lightCount++;}
				else if(u.getType() == rangedType) {rangedCount++;}
			}
		}
		
		if(tileCount > 100)
		{
			for(Unit barrack : barracks)
			{
				//Train a light type then a ranged type
				
				if(rangedCount < lightCount)
				{
					if(p.getResources() >= rangedType.cost)
					{
						trainTowardEnemy(utt, barrack, rangedType, p.getID());
					}
				}else
				{
					if(p.getResources() >= lightType.cost)
					{
						trainTowardEnemy(utt, barrack, lightType, p.getID());
					}
				}
			}
		}else
		{
			for(Unit barrack : barracks)
			{
				if(p.getResources() >= heavyType.cost)
				{
					trainTowardEnemy(utt, barrack, heavyType, p.getID());
				}
			}
		}
		
	}
	
	public void flankAttack(List<Unit> attackers, Player p, PhysicalGameState pgs)
	{
		int enemyBaseX = 0, enemyBaseY = 0;
		
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == baseType)
			{
				if(u.getPlayer() != p.getID())
				{
					enemyBaseX = u.getX();
					enemyBaseY = u.getY();
				}
			}
		}
		
		int halfMap = pgs.getHeight()/2;
		
		List<Unit> unitsReadyToAttack = new LinkedList<Unit>();
		for(Unit u : attackers)
		{
			if(enemyBaseX != 0 && enemyBaseY != 0)
			{
				int distanceToBase = Math.abs(enemyBaseX - u.getX()) + Math.abs(enemyBaseY - u.getY());
				if(distanceToBase > halfMap)
				{
					if(u.getX() != enemyBaseX)
					{
						move(u, enemyBaseX, u.getY());
					}else if(u.getY() != enemyBaseY)
					{
						attack(u, pgs.getUnitAt(enemyBaseX, enemyBaseY));
					}
				}else {unitsReadyToAttack.add(u);}
			}else {unitsReadyToAttack.add(u);}
		}
		attackClosest(unitsReadyToAttack, p, pgs);
	}
	
	/*public void wallBehaviour(List<Unit> wall, Player p, PhysicalGameState pgs)
	{
		if(wall.isEmpty()) {return;}
		
		
		if(wallIsMoving)
		{
			for(Unit u: wall)
			{
				move(u, u.getX() +3, u.getY());
			}
		}else
		{
			for(Unit u : wall)
			{
				Unit closestEnemy = null;
				int closestDist = 0;
				int mapMiddle = pgs.getWidth() / 2;
				
				int posCounter = 0;
				for(Unit u2: pgs.getUnits())
				{					
					if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
					{
						if(u.getY() != currWallOffset)
						{							
							move(u, mapMiddle + posCounter, currWallOffset);
						}else {
							int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
							if(d < closestDist || closestEnemy == null)
							{
								closestEnemy = u2;
								closestDist = d;
							}
							
							if(closestEnemy != null)
							{
								noMoveAttack(u, closestEnemy);
							}
						}
					}
					posCounter ++;
				}
			}
		}
	}*/
	
	public void attackClosest(List<Unit> attackers, Player p, PhysicalGameState pgs)
	{
		for(Unit u : attackers)
		{
			Unit closestEnemy = null;
			int closestDist = 0;
			
			for(Unit u2: pgs.getUnits())
			{
				if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if(d < closestDist || closestEnemy == null)
					{
						closestEnemy = u2;
						closestDist = d;
					}
					
					if(closestEnemy != null)
					{
						maxAttack(u, closestEnemy);
					}
				}
			}
		}
	}
	
	public void workerBehaviour(List<Unit> workers, Player p, PhysicalGameState pgs)
	{
		int baseNum = 0;
		int barrackNum = 0;
		int workerNum = 0;
		
		List<Unit> freeWorkers = new LinkedList<Unit>();
		freeWorkers.addAll(workers);
		
		if(workers.isEmpty()) {return;}
		
		for (Unit u2 : pgs.getUnits())
		{
			if(u2.getPlayer() == p.getID())
			{
				if(u2.getType() == baseType) {baseNum++;}
				else if(u2.getType() == barracksType) {barrackNum++;}
				else if(u2.getType() == workerType) {workerNum++;}
			}
		}
		
		List<Integer> reservedPositions = new LinkedList<Integer>();
		if(baseNum == 0 && !freeWorkers.isEmpty()) 
		{
			if(p.getResources() >= baseType.cost)
			{
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
			}
		}
		if(barrackNum == 0 && !freeWorkers.isEmpty())
		{
			//Build barracks immediatley if the map is big enough.
			if(tileCount > 100)
			{
				if(p.getResources() >= barracksType. cost )
				{
					//Get the position of our base.
					int basePosX = 0, basePosY = 0;
					for(Unit u3 : pgs.getUnits())
					{
						if(u3.getType() == baseType && u3.getPlayer() == p.getID())
						{
							basePosX = u3.getX();
							basePosY = u3.getY();
						}
					}
					
					
					Unit u = freeWorkers.remove(0);
					buildIfNotAlreadyBuilding(u, barracksType, basePosX, basePosY + 2, reservedPositions, p, pgs);
				}
			}else
			{
				if(p.getResources() >= barracksType.cost + 4)
				{
					int basePosX = 0, basePosY = 0;
					for(Unit u3 : pgs.getUnits())
					{
						if(u3.getType() == baseType && u3.getPlayer() == p.getID())
						{
							basePosX = u3.getX();
							basePosY = u3.getY();
						}
					}
					
					
					Unit u = freeWorkers.remove(0);
					buildIfNotAlreadyBuilding(u, barracksType, basePosX, basePosY + 2, reservedPositions, p, pgs);
				}
			}
			
		} 
		
		for (Unit u : freeWorkers)
		{
			Unit closestBase = null;
			Unit closestResource = null;
			int closestDistance = 0;
			for(Unit u2 : pgs.getUnits())
			{
				if(u2.getType().isResource)
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if(closestResource == null || d < closestDistance)
					{
						closestResource = u2;
						closestDistance = d;
					}
				}
			}
			closestDistance = 0;
			for(Unit u2 : pgs.getUnits())
			{
				if(u2.getType().isStockpile && u2.getPlayer() == p.getID())
				{
					int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
					if(closestBase == null || d < closestDistance)
					{
						closestBase = u2;
						closestDistance = d;
					}
				}
			}
			
			if(closestResource != null && closestBase != null)
			{
				AbstractAction aa = getAbstractAction(u);
				if(aa instanceof Harvest)
				{
					Harvest h_aa = (Harvest)aa;
					if(h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {harvest (u, closestResource, closestBase);}
				}else {harvest(u, closestResource, closestBase);}
			}
		}
	}
	
	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return new Wanli(utt,pf);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		List<ParameterSpecification> parameters = new ArrayList<>();
		parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
		
		
		return null;
	}

}
