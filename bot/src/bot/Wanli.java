package bot;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import extractions.AbstractAction;
import extractions.AbstractionLayerAICustom;
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

public class Wanli extends AbstractionLayerAICustom
{
	//Variables
	protected UnitTypeTable utt;
	UnitType workerType;
	UnitType baseType;
	UnitType barracksType;
	UnitType rangedType;
	UnitType lightType;
	UnitType heavyType;
	
	int tileCount = 0;
	
	//Const ints
	int maxNumberCollecterWorkers = 3;
	int maxNumberAttackWorkers = 4;
	int NoWhereMapSize = 72;
	
	
	
	//Store list of units globally
	List<Unit> workers = new LinkedList<Unit>();
	List<Unit> bases = new LinkedList<Unit>();
	List<Unit> barracks = new LinkedList<Unit>();
	List<Unit> heavyUnits = new LinkedList<Unit>();
	List<Unit> rangedUnits = new LinkedList<Unit>();
	List<Unit> lightUnits = new LinkedList<Unit>();
	List<Unit> enemyUnits = new LinkedList<Unit>();
	
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
		
		//Unit lists
		ListCurrentUnitsByType(p, pgs);
		
		//Decide what kind of strategy to use based on the size of the current map
		if(tileCount <= 100)
		{
			workerRushActions(p, pgs);
		}else
		{
			flankActions(p, pgs);
		}
		
		return translateActions(player,gs);
	}
  
	//Dictates the actions of units for smaller maps. Concentrates on pure rush strategies.
	public void workerRushActions(Player p, PhysicalGameState pgs)
	{
		List<Unit> collecter = new LinkedList<Unit>();
		List<Unit> rushAttackers = new LinkedList<Unit>();
		
		//List out all the units.
		for(Unit u: workers)
		{
			if(collecter.size() < maxNumberCollecterWorkers / 2)
			{
				collecter.add(u);
			}else
			{
				rushAttackers.add(u);
			}
		}
		
		rushAttackers.addAll(heavyUnits);
		rushAttackers.addAll(rangedUnits);
		//Dictate units behvaiours.
		workerBehaviour(collecter, p, pgs);
		//Use different rush strategy if on 9x8 map.
		if(tileCount == NoWhereMapSize)
		{
			attackClosest(rushAttackers, enemyUnits, p, pgs);
		}else
		{
			flankAttack(rushAttackers, enemyUnits, p, pgs);
		}
		baseBehaviour(bases, p, pgs);
		barrackBehaviour(barracks, p , pgs);
	}

	
	public void flankActions(Player p, PhysicalGameState pgs)
	{
		//Lists to store units to dictate behaviour
		List<Unit> collecters = new LinkedList<Unit>();	
		List<Unit> rushAttackers = new LinkedList<Unit>();
		List<Unit> flankAttackers = new LinkedList<Unit>();
		//Sort worker units into different lists.
		for(Unit u : workers)
		{
			if(collecters.size() < maxNumberCollecterWorkers)
			{
				collecters.add(u);
			}else
			{
				rushAttackers.add(u);
			}
		}
		
		flankAttackers.addAll(rangedUnits);
		flankAttackers.addAll(lightUnits);
		
		//Dictate units behaviours
		workerBehaviour(collecters, p, pgs);
		attackClosest(rushAttackers, enemyUnits, p, pgs);
		baseBehaviour(bases, p, pgs);
		barrackBehaviour(barracks, p, pgs);
		flankAttack(flankAttackers, enemyUnits, p, pgs);
	}
	//Lists all our teams units by team and makes a list of enemy units.
	public void ListCurrentUnitsByType(Player p, PhysicalGameState pgs)
	{
		//Empty unit lists. 
		workers.clear();
		bases.clear();
		barracks.clear();
		heavyUnits.clear();
		rangedUnits.clear();
		lightUnits.clear();
		enemyUnits.clear();
		
		//Sort units into appropriate lists.
		for(Unit u : pgs.getUnits())
		{
			if(u.getPlayer() == p.getID()) {

				if(u.getType() == workerType){workers.add(u);}
				else if(u.getType() == baseType){bases.add(u);}
				else if(u.getType() == barracksType){barracks.add(u);}
				else if(u.getType() == rangedType) {rangedUnits.add(u);}
				else if(u.getType() == lightType) {lightUnits.add(u);}
				else if(u.getType() == heavyType) {heavyUnits.add(u);}
			
			}else if(u.getPlayer() >= 0 && u.getPlayer() != p.getID()){	enemyUnits.add(u);}
		}
	}
	
	
	//What should bases be doing
	public void baseBehaviour(List<Unit> bases, Player p, PhysicalGameState pgs)
	{
		//Don't do anything if there arn't any bases.
		if(bases.isEmpty()) {return;}
		
		//Count the current number of workers we own.
		int numOfWorkers = 0;
		for(Unit u : pgs.getUnits())
		{
			if(u.getPlayer() == p.getID() && u.getType() == workerType) {numOfWorkers++;}
		}
		
		for(Unit base : bases)
		{
			if(tileCount != NoWhereMapSize)
			{
				//Train more workers if there are not enough.
				if(p.getResources() >= workerType.cost && numOfWorkers <= maxNumberCollecterWorkers + maxNumberAttackWorkers)
				{
					trainTowardEnemy(utt, base, workerType, p.getID());
				}
			}else
			{
				if(p.getResources() >= workerType.cost && numOfWorkers < maxNumberCollecterWorkers)
				{
					train(base, workerType);
				}
			}
		}
	}
	//How do barracks behave.
	public void barrackBehaviour(List<Unit> barracks, Player p, PhysicalGameState pgs)
	{
		//Count the number of light and ranged units we own.
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
		//Decide behaviour based on map size.
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
		}else if(tileCount == NoWhereMapSize)
		{
			for(Unit barrack : barracks)
			{
				if(p.getResources() >= rangedType.cost)
				{
					trainTowardEnemy(utt, barrack, rangedType, p.getID());
				}
			}
		}
		else
		{
			//Train just heavy units.
			for(Unit barrack : barracks)
			{
				if(p.getResources() >= heavyType.cost)
				{
					trainTowardEnemy(utt, barrack, heavyType, p.getID());
				}
			}
		}
		
	}
	//Get units to move to the side of the enemy base before attacking.
	public void flankAttack(List<Unit> attackers, List<Unit> enemies, Player p, PhysicalGameState pgs)
	{
		Unit enemyBase = null;
		Unit ourBase = null;
		for(Unit u : pgs.getUnits())
		{
			if(u.getType() == baseType)
			{
				if(u.getPlayer() != p.getID())
				{
					enemyBase = u;
				}else
				{
					ourBase = u;
				}
			}
		}
		//Find the halfway point of the map
		int halfMap = pgs.getHeight()/2;
		int halfMapW = pgs.getWidth()/2;
		//Get a list of units who are close enough to attack
		List<Unit> unitsReadyToAttack = new LinkedList<Unit>();
		for(Unit u : attackers)
		{
			boolean overrideMovement = false;
			int closestDistance = 0;
			for(Unit u2 : enemies)
			{
				if(u2.getType() == workerType)
				{
					int d = CalcDistance(u, u2);
					if(d < closestDistance || closestDistance == 0)
					{
						closestDistance = d;
					}
				}
			}
			if(closestDistance <= u.getAttackRange() + 1)
			{
				unitsReadyToAttack.add(u);
				overrideMovement = true;
			}
			
			
			
			if(enemyBase != null && ourBase != null && !overrideMovement)
			{
				int distanceToBase = CalcDistance(u, enemyBase);
				if(distanceToBase > halfMap)
				{
					if(ourBase.getX() < halfMapW)
					{
						//Left side
						XtoY(u, enemyBase, pgs);
					}else
					{
						//Right side
						YtoX(u, enemyBase, pgs);
					}
				}else {unitsReadyToAttack.add(u);}
			}else {unitsReadyToAttack.add(u);}
			
		}
		attackClosest(unitsReadyToAttack,enemies, p, pgs);
	}
	//Gets the unit to move toward the X position of the enemy base first.
	public void XtoY(Unit u, Unit enemyBase, PhysicalGameState pgs)
	{
		if(u.getX() != enemyBase.getX())
		{
			move(u, enemyBase.getX(), u.getY());
		}else if(u.getY() != enemyBase.getY())
		{
			//Used to move unit toward enemy base. As move to a spot that is filled dosn't work.
			attack(u, enemyBase);
		}
	}
	//Gets the unit to move to the Y position of the enemy base first.
	public void YtoX(Unit u, Unit enemyBase, PhysicalGameState pgs)
	{
		if(u.getY() != enemyBase.getY())
		{
			move(u, u.getX(), enemyBase.getY());
		}else if(u.getX() != enemyBase.getX())
		{
			attack(u, enemyBase);
		}
	}
	
	//Get the closes enemy unit and attack it.
	public void attackClosest(List<Unit> attackers, List<Unit> enemies, Player p, PhysicalGameState pgs)
	{
		for(Unit u : attackers)
		{
			//Find the closest enemy unit for each unit given.
			Unit closestEnemy = null;
			int closestDist = 0;
			for(Unit u2: enemies)
			{
				if(u2.getPlayer() >= 0 && u2.getPlayer() != p.getID())
				{
					int d = CalcDistance(u, u2);
					if(d < closestDist || closestEnemy == null)
					{
						closestEnemy = u2;
						closestDist = d;
					}
					
					if(closestEnemy != null)
					{
						attack(u, closestEnemy);
					}
				}
			}
		}
	}
	//Dictate behaviour of non attacking worker units.
	public void workerBehaviour(List<Unit> workers, Player p, PhysicalGameState pgs)
	{
		int baseNum = 0;
		int barrackNum = 0;
		//Stores a list of workers that are sorted to store only worker units which are avaliable to collect resources.
		List<Unit> freeWorkers = new LinkedList<Unit>();
		freeWorkers.addAll(workers);
		
		if(workers.isEmpty()) {return;}
		//Count bases and barracks we own.
		for (Unit u2 : pgs.getUnits())
		{
			if(u2.getPlayer() == p.getID())
			{
				if(u2.getType() == baseType) {baseNum++;}
				else if(u2.getType() == barracksType) {barrackNum++;}
			}
		}
		
		//Build a base if there isn't one.
		List<Integer> reservedPositions = new LinkedList<Integer>();
		if(baseNum == 0 && !freeWorkers.isEmpty()) 
		{
			if(p.getResources() >= baseType.cost)
			{
				Unit u = freeWorkers.remove(0);
				buildIfNotAlreadyBuilding(u, baseType, u.getX(), u.getY(), reservedPositions, p, pgs);
			}
		}
		//Build a barracks if there isn't one.
		if(barrackNum == 0 && !freeWorkers.isEmpty())
		{
			//Build barracks immediatley if the map is big enough.
			//Also do this when it is the 9x8 map, as there is enough time to build up units on that one.
			if(tileCount > 100 || tileCount == NoWhereMapSize)
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
			}
			//Build a barracks after having collected a resource buffer if the map is not 8x8 size.
			else if (tileCount > 64)
			{
				//Wait until resources are built up to build a barracks.
				//Gives the base resources to train workers to defend while the barracks is built
				int resourceBuffer = 4;

				if(p.getResources() >= barracksType.cost + resourceBuffer)
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
		//List out the workers left who are not building to collect resources.
		//If there is no base make a list of these units to get them to attack the closet enemy
		List<Unit> toAttack = new LinkedList<Unit>();
		for (Unit u : freeWorkers)
		{
			//Find the closest resource for each unit.
			Unit closestBase = null;
			Unit closestResource = null;
			int closestDistance = 0;
			for(Unit u2 : pgs.getUnits())
			{
				if(u2.getType().isResource)
				{
					int d = CalcDistance(u, u2);
					if(closestResource == null || d < closestDistance)
					{
						closestResource = u2;
						closestDistance = d;
					}
				}
			}
			closestDistance = 0;
			//Find the closest base for each unit.
			for(Unit u2 : pgs.getUnits())
			{
				if(u2.getType().isStockpile && u2.getPlayer() == p.getID())
				{
					int d = CalcDistance(u, u2);
					if(closestBase == null || d < closestDistance)
					{
						closestBase = u2;
						closestDistance = d;
					}
				}
			}
			//Check if there is a resource and base, if so harvest.
			//If there is not a base and a resource then attack the closest enemy.
			if(closestResource != null && closestBase != null)
			{
				AbstractAction aa = getAbstractAction(u);
				if(aa instanceof Harvest)
				{
					Harvest h_aa = (Harvest)aa;
					if(h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {harvest (u, closestResource, closestBase);}
				}else {harvest(u, closestResource, closestBase);}
			}else
			{
				toAttack.add(u);
			}
		}
		attackClosest(toAttack, enemyUnits, p, pgs);
	}
	
	//Find the distance between two enemies.
	public int CalcDistance(Unit a, Unit b)
	{
		int distance = Math.abs(b.getX() - a.getX()) + Math.abs(b.getY() - a.getY());
		return distance;
	}
	
	@Override
	public AI clone() 
	{
		return new Wanli(utt,pf);
	}

	@Override
	public List<ParameterSpecification> getParameters() 
	{
		List<ParameterSpecification> parameters = new ArrayList<>();
		parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));
		
		
		return null;
	}

}
