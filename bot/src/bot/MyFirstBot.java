package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Attack;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.*;
import util.XMLWriter;


public class MyFirstBot extends AbstractionLayerAI
{
	Random r = new Random();
	protected UnitTypeTable utt;
	UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType rangedType;
    UnitType lightType;
    UnitType heavyType;
	
    //Stats trackers
    int workersTrained = 0;
    int rangedTrained = 0;
    int lightTrained = 0;
    int heavyTrained = 0;
    
    int basesBuilt = 0;
    int barracksBuilt = 0;
    
    public MyFirstBot(UnitTypeTable a_utt)
    {
    	this(a_utt, new AStarPathFinding());
    }
    
	public MyFirstBot(UnitTypeTable a_utt ,PathFinding a_pf) {
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
		// TODO Auto-generated method stub
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Player p = gs.getPlayer(player);
		//Base behaviour
		for(Unit u : pgs.getUnits()) 
		{
			if(u.getType() == baseType && u.getPlayer() == player && gs.getActionAssignment(u) == null)
			{baseBehavior(u, p, pgs);}
		}
		//Barracks behaviour
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == barracksType && u.getPlayer() == player && gs.getActionAssignment(u) == null) {barrackBehavior(u, p, pgs);}
		}
		//behavior of attack units
		for(Unit u : pgs.getUnits())
		{
			if(u.getType().canAttack && u.getType() == rangedType && !u.getType().canHarvest && u.getPlayer() == player && gs.getActionAssignment(u) == null) {turretUnitBehaviour(u, p, gs);}
			else if(u.getType().canAttack && !u.getType().canHarvest && u.getPlayer() == player && gs.getActionAssignment(u) == null) {meleeUnitBehavior(u, p, gs);}//meleeUnitBehavior
		}
		
		//Worker behaviours.
		List<Unit> workers = new LinkedList<Unit>();
		for(Unit u : pgs.getUnits())
		{
			if(u.getType().canHarvest && u.getPlayer() == player) {workers.add(u);}
		}
		workerBehavior(workers,p,pgs);
		
		return translateActions(player,gs);
	}

	//Unit actions
	public void baseBehavior(Unit u, Player p, PhysicalGameState pgs)
	{
		int nworkers = 0;
		for (Unit u2 : pgs.getUnits())
		{
			if(u2.getType() == workerType && u2.getPlayer() == p.getID()) {nworkers++;}
		}
		if(nworkers < 2 && p.getResources() >= workerType.cost) {train(u, workerType);}
	}
	
	public void barrackBehavior(Unit u, Player p, PhysicalGameState pgs)
	{
		
		if(p.getResources() >= rangedType.cost && rangedTrained == 0) {train(u, rangedType); rangedTrained++;}
		
		else if(p.getResources() >= lightType.cost && rangedTrained > 0) {train(u, lightType); lightTrained++;}
	}
	
	public void meleeUnitBehavior(Unit u, Player p, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Unit closestEnemy = null;
		int closestDistance = 0;
		
		for(Unit u2 : pgs.getUnits())
		{
			if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
			{
				int d = Math.abs(u2.getX() - u.getX()) - Math.abs(u2.getY() - u.getY());
				if(closestEnemy == null || d < closestDistance)
				{
					closestEnemy = u2;
					closestDistance = d;
				}
			}
		}
		if(closestEnemy != null)
		{
			attack (u, closestEnemy);
		}
	}
	
	//Used ranged units as turrets to set up defensive stuff.
	public void turretUnitBehaviour(Unit u, Player p, GameState gs)
	{
		PhysicalGameState pgs = gs.getPhysicalGameState();
		Unit closestEnemy = null;
		int closestDistance = 0;
		//Find the closest enemy.
		for(Unit u2 : pgs.getUnits())
		{
			if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
			{
				int d = (u2.getX() - u.getX()) + (u2.getY() - u.getY());
				d = d / 2;
				if(closestEnemy == null || d < closestDistance)
				{
					closestEnemy = u2;
					closestDistance = d;
				}
			}
		}
		//Do nothing while closest enemy is not in range.
		if(closestDistance + 1 >= u.getAttackRange())
		{
			idle(u);
		}
		//Attack closest enemy when in range.
		else if(closestEnemy != null)
		{
			attack(u, closestEnemy);
		}
	}
	
	public void workerBehavior(List<Unit> workers, Player p, PhysicalGameState pgs)
	{
		int baseNum = 0;
		int barrackNum = 0;
		int usedResourceNum = 0;
		
		List<Unit> freeWorkers = new LinkedList<Unit>();
		freeWorkers.addAll(workers);
		
		if(workers.isEmpty()) {return;}
		
		for (Unit u2 : pgs.getUnits())
		{
			if(u2.getType() == baseType && u2.getPlayer() == p.getID()) {baseNum++;}
			if(u2.getType() == barracksType && u2.getPlayer() == p.getID()) {barrackNum++;}
		}
		
		List<Integer> reservedPositions = new LinkedList<Integer>();
		if(baseNum == 0 && !freeWorkers.isEmpty()) 
		{
			if(p.getResources() >= baseType.cost)
			{
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
				usedResourceNum += baseType.cost;
				basesBuilt++;
			}
		}
		if(barrackNum == 0 && !freeWorkers.isEmpty())
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
				usedResourceNum += barracksType.cost;
				barracksBuilt++;
			}
		} else if(barrackNum == 1 && lightTrained > 1 && !freeWorkers.isEmpty())
		{
			if(p.getResources() >= barracksType. cost )
			{
				//Get the position of our other barracks.
				int barrackPosX = 0, barrackPosY = 0;
				for(Unit u3 : pgs.getUnits())
				{
					if(u3.getType() == barracksType && u3.getPlayer() == p.getID())
					{
						barrackPosX = u3.getX();
						barrackPosY = u3.getY();
					}
				}
				
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, barracksType, barrackPosX, barrackPosY + 1, reservedPositions, p, pgs);
				usedResourceNum += barracksType.cost;
				barracksBuilt++;
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
		return new MyFirstBot(utt,pf);
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO Auto-generated method stub
		List<ParameterSpecification> parameters = new ArrayList<>();
		
		parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
		
		return null;
	}
	
}