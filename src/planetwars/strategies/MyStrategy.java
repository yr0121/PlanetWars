package planetwars.strategies;

import planetwars.publicapi.*;

import java.util.*;

/**
 * * Created by yuanhaoruan on 12/1/17.
 * ruanx054
 * */
public class MyStrategy implements IStrategy {

        /* Method that performs the move in the game
           @param planets a List represents the planets in current game
           @param planetOperations a IplanetOperattions to trasfer people from one planet to another
           @param eventsToExecute a Queue to add moves to
         */

    @Override
    public void takeTurn(List<IPlanet> planets, IPlanetOperations planetOperations, Queue<IEvent> eventsToExecute) {
        long number = 0;
        HashMap<Integer, IPlanet> neutralMap = new HashMap<>();
        HashMap<Integer, IPlanet> opponentMap = new HashMap<>();
        List<IVisiblePlanet> conqueredVisiblePlanets = new ArrayList<>();
        List<IVisiblePlanet> unconqueredPlanets = new ArrayList<>();
        for (IPlanet planet : planets) {
            if (planet instanceof IVisiblePlanet) {
                if (((IVisiblePlanet) planet).getOwner() == Owner.SELF) {
                    conqueredVisiblePlanets.add((IVisiblePlanet) planet);
                } else {
                    if (((IVisiblePlanet) planet).getOwner() == Owner.NEUTRAL) {
                        neutralMap.put(planet.getId(), planet);
                        unconqueredPlanets.add((IVisiblePlanet) planet);
                    } else {
                        opponentMap.put(planet.getId(), planet);
                        unconqueredPlanets.add((IVisiblePlanet) planet);
                    }
                }
            }
        }
        for (IVisiblePlanet planet1 : conqueredVisiblePlanets) {
            PriorityQueue planetQueue = breadthTraversal(planets, planet1);
            while (!planetQueue.isEmpty()) {
                IPlanet planet2 = (IPlanet) planetQueue.poll();
                if (planet2 instanceof IVisiblePlanet) {
                    if ((neutralMap.containsValue(planet2))) {
                        if (canDefend(planet1)) {
                            number = getNumber(planet1, (IVisiblePlanet) planet2);
                            eventsToExecute.offer(planetOperations.transferPeople(planet1, planet2, number));
                        }
                    } else if (opponentMap.containsValue((planet2))) {
                        if (canDefend(planet1)) {
                            if (canAttack(planet1, (IVisiblePlanet) planet2)) {
                                number = getNumber(planet1, (IVisiblePlanet) planet2);
                                eventsToExecute.offer(planetOperations.transferPeople(planet1, planet2, number));
                            }
                        }
                    } else {
                        if (canDefend(planet1)) {
                            if (canAttack(planet1, (IVisiblePlanet) planet2)) {
                                number = getNumber(planet1, (IVisiblePlanet) planet2);
                                eventsToExecute.offer(planetOperations.transferPeople(planet1, planet2, number));
                            }
                        }
                    }
                }
            }
        }
    }


    /* Method that determines if a planet can defend and protect its own
       @param source an IvisiblePlanet that represents the planets we want to check
       @returns true if can defend, otherwise false
    */
    public boolean canDefend(IVisiblePlanet source) {
        boolean result = true;
        long number = source.getPopulation();
        long attackNumber = 0;
        List<IShuttle> shuttleList = source.getIncomingShuttles();
        for (IShuttle shuttle : shuttleList) {
            if (shuttle.getTurnsToArrival() == 0) {
                if (shuttle.getOwner() == Owner.SELF) {
                    number = number + shuttle.getNumberPeople();
                }
                if (shuttle.getOwner() == Owner.OPPONENT) {
                    attackNumber = shuttle.getNumberPeople();
                }
            }
        }
        if (attackNumber > number * 1.1) {
            result = false;
        }
        return result;
    }

    /* Method that determines if one planet can attack another
       @param source planet which initiates the attack
       @param target planet which need to defend the attack of source
       @returns true if can attack, otherwise false
    */
    public boolean canAttack(IVisiblePlanet source, IVisiblePlanet target) {
        boolean result = false;
        long attackNumber = source.getPopulation();
        long number = target.getPopulation();
        List<IShuttle> shuttleList1 = target.getIncomingShuttles();
        for (IShuttle shuttle : shuttleList1) {
            if (shuttle.getTurnsToArrival() == 0) {
                if (shuttle.getOwner() == Owner.OPPONENT) {
                    number = number + shuttle.getNumberPeople();
                } else {
                    attackNumber = attackNumber + shuttle.getNumberPeople();
                }
            }
        }
        if (attackNumber > number * 1.1) {
            result = true;
        }
        return result;
    }


    /* Method that find the neighbors planets of one planet
        @param planets List represents the plants in the graph
        @param p a planet that needs to find its neighbor planets
        @returns List of planets that are the neighbors of p
    */
    public List<IPlanet> getNeighbors(List<IPlanet> planets, IPlanet p) {
        List<Integer> result = new ArrayList<>();
        List<IPlanet> neighbors = new ArrayList<>();
        Set<IEdge> theEdge = p.getEdges();
        for (IEdge e : theEdge) {
            result.add(e.getDestinationPlanetId());
        }
        for (IPlanet planet : planets) {
            if (result.contains(planet.getId())) {
                neighbors.add(planet);
            }
        }
        return neighbors;
    }

    /* Method traverse the planets starting from a source planet
       @param planets List represents all the plants in the graph
       @param source planet represents the plant to start in the traversal process
       @returns Priority Queue that holds the planets of a specific order
    */
    public PriorityQueue<IPlanet> breadthTraversal(List<IPlanet> planets, IPlanet source) {
        neighborsComparator comparator = new neighborsComparator();
        PriorityQueue<IPlanet> traversalOrder = new PriorityQueue(planets.size(), comparator.reversed());
        Queue<IPlanet> vertexQueue = new LinkedList<>();
        HashMap<IPlanet, Boolean> planetMap = new HashMap<IPlanet, Boolean>();
        planetMap.put(source, Boolean.TRUE);
        vertexQueue.add(source);
        traversalOrder.add(source);
        while (!vertexQueue.isEmpty()) {
            IPlanet frontVertex = vertexQueue.poll();
            Iterator<IPlanet> itr = getNeighbors(planets, frontVertex).iterator();
            while (itr.hasNext()) {
                IPlanet nextNeighbor = itr.next();
                if (planetMap.get(nextNeighbor) != Boolean.TRUE) {
                    planetMap.put(nextNeighbor, Boolean.TRUE);
                    traversalOrder.add(nextNeighbor);
                    vertexQueue.add(nextNeighbor);
                }
            }
        }
        return traversalOrder;
    }

    /* Method determines how many number should send from one planet to another
       @param source a IVisiblePlanet that sends people out
       @param target a IVisiblePlanet that receives people from source
       @returns long number of people need to send
    */
    public long getNumber(IVisiblePlanet source, IVisiblePlanet target) {
        long number = 0;
        if (target.getOwner() == Owner.NEUTRAL && source.getPopulation() > 2) {
            number = 1;
        }
        if (target.getOwner() == Owner.OPPONENT) {
            if (source.getPopulation() > target.getPopulation() * 1.1+ 1) {
                number = (long) (target.getPopulation() * 1.1);
            } else {
                number = (long) (target.getPopulation() * 0.1+1);
            }
        }
        if (target.getOwner() == Owner.SELF) {
                number = (long) (source.getSize());
            }
        return number;
    }


    /* Method that get the name of the strategy
       @returns String name of the strategy
    */
    @Override
    public String getName() {
        return "Yuanhao-Strategy";
    }


    /* Method that determines if want to participate competition
       @returns true if want to participate competition, otherwaise false
    */
    @Override
    public boolean compete() {
        return false;
    }

    private class neighborsComparator implements Comparator<IPlanet> {

        @Override
        public int compare(IPlanet p1, IPlanet p2) {
            long number1 = p1.getEdges().size();
            long number2 = p2.getEdges().size();
            if (number1 < number2) {
                return -1;
            }
            if (number1 > number2) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}








