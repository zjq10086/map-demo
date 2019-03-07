package com.example.demo.voronoi;

/*
 * Copyright (c) 2005, 2007 by L. Paul Chew.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

import com.example.demo.util.JsonUtil;
import com.google.common.primitives.Doubles;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A 2D Delaunay Triangulation (DT) with incremental site insertion.
 *
 * This is not the fastest way to build a DT, but it's a reasonable way to build
 * a DT incrementally and it makes a nice interactive display. There are several
 * O(n log n) methods, but they require that the sites are all known initially.
 *
 * A Triangulation is a Set of Triangles. A Triangulation is unmodifiable as a
 * Set; the only way to change it is to add sites (via delaunayPlace).
 *
 * @author Paul Chew
 *
 * Created July 2005. Derived from an earlier, messier version.
 *
 * Modified November 2007. Rewrote to use AbstractSet as parent class and to use
 * the Graph class internally. Tried to make the DT algorithm clearer by
 * explicitly creating a cavity.  Added code needed to find a Voronoi cell.
 *
 */
public class Triangulation extends AbstractSet<Triangle> {

    private Triangle mostRecent = null;      // Most recently "active" triangle
    private Graph<Triangle> triGraph;        // Holds triangles for navigation

    /**
     * All sites must fall within the initial triangle.
     * @param triangle the initial triangle
     */
    public Triangulation(Triangle triangle) {
        triGraph = new Graph<Triangle>();
        triGraph.add(triangle);
        mostRecent = triangle;
    }

    /* The following two methods are required by AbstractSet */

    @Override
    public Iterator<Triangle> iterator () {
        return triGraph.nodeSet().iterator();
    }

    @Override
    public int size () {
        return triGraph.nodeSet().size();
    }

    @Override
    public String toString () {
        return "Triangulation with " + size() + " triangles";
    }

    /**
     * True iff triangle is a member of this triangulation.
     * This method isn't required by AbstractSet, but it improves efficiency.
     * @param triangle the object to check for membership
     */
    public boolean contains (Object triangle) {
        return triGraph.nodeSet().contains(triangle);
    }

    /**
     * Report neighbor opposite the given vertex of triangle.
     * @param site a vertex of triangle
     * @param triangle we want the neighbor of this triangle
     * @return the neighbor opposite site in triangle; null if none
     * @throws IllegalArgumentException if site is not in this triangle
     */
    public Triangle neighborOpposite (Pnt site, Triangle triangle) {
        if (!triangle.contains(site))
            throw new IllegalArgumentException("Bad vertex; not in triangle");
        for (Triangle neighbor: triGraph.neighbors(triangle)) {
            if (!neighbor.contains(site)) return neighbor;
        }
        return null;
    }

    /**
     * Return the set of triangles adjacent to triangle.
     * @param triangle the triangle to check
     * @return the neighbors of triangle
     */
    public Set<Triangle> neighbors(Triangle triangle) {
        return triGraph.neighbors(triangle);
    }

    /**
     * Report triangles surrounding site in order (cw or ccw).
     * @param site we want the surrounding triangles for this site
     * @param triangle a "starting" triangle that has site as a vertex
     * @return all triangles surrounding site in order (cw or ccw)
     * @throws IllegalArgumentException if site is not in triangle
     */
    public List<Triangle> surroundingTriangles (Pnt site, Triangle triangle) {
        if (!triangle.contains(site))
            throw new IllegalArgumentException("Site not in triangle");
        List<Triangle> list = new ArrayList<Triangle>();
        Triangle start = triangle;
        Pnt guide = triangle.getVertexButNot(site);        // Affects cw or ccw
        while (true) {
            list.add(triangle);
            Triangle previous = triangle;
            triangle = this.neighborOpposite(guide, triangle); // Next triangle
            guide = previous.getVertexButNot(site, guide);     // Update guide
            if (triangle == start) break;
        }
        return list;
    }

    /**
     * Locate the triangle with point inside it or on its boundary.
     * @param point the point to locate
     * @return the triangle that holds point; null if no such triangle
     */
    public Triangle locate (Pnt point) {
        Triangle triangle = mostRecent;
        if (!this.contains(triangle)) triangle = null;

        // Try a directed walk (this works fine in 2D, but can fail in 3D)
        Set<Triangle> visited = new HashSet<Triangle>();
        while (triangle != null) {
            if (visited.contains(triangle)) { // This should never happen
                System.out.println("Warning: Caught in a locate loop");
                break;
            }
            visited.add(triangle);
            // Corner opposite point
            Pnt corner = point.isOutside(triangle.toArray(new Pnt[0]));
            if (corner == null) return triangle;
            triangle = this.neighborOpposite(corner, triangle);
        }
        // No luck; try brute force
        System.out.println("Warning: Checking all triangles for " + point);
        for (Triangle tri: this) {
            if (point.isOutside(tri.toArray(new Pnt[0])) == null) return tri;
        }
        // No such triangle
        System.out.println("Warning: No triangle holds " + point);
        return null;
    }

    /**
     * Place a new site into the DT.
     * Nothing happens if the site matches an existing DT vertex.
     * @param site the new Pnt
     * @throws IllegalArgumentException if site does not lie in any triangle
     */
    public void delaunayPlace (Pnt site) {
        // Uses straightforward scheme rather than best asymptotic time

        // Locate containing triangle
        Triangle triangle = locate(site);
        // Give up if no containing triangle or if site is already in DT
        if (triangle == null)
            throw new IllegalArgumentException("No containing triangle");
        if (triangle.contains(site)) return;

        // Determine the cavity and update the triangulation
        Set<Triangle> cavity = getCavity(site, triangle);
        mostRecent = update(site, cavity);
    }

    /**
     * Determine the cavity caused by site.
     * @param site the site causing the cavity
     * @param triangle the triangle containing site
     * @return set of all triangles that have site in their circumcircle
     */
    private Set<Triangle> getCavity (Pnt site, Triangle triangle) {
        Set<Triangle> encroached = new HashSet<Triangle>();
        Queue<Triangle> toBeChecked = new LinkedList<Triangle>();
        Set<Triangle> marked = new HashSet<Triangle>();
        toBeChecked.add(triangle);
        marked.add(triangle);
        while (!toBeChecked.isEmpty()) {
            triangle = toBeChecked.remove();
            if (site.vsCircumcircle(triangle.toArray(new Pnt[0])) == 1)
                continue; // Site outside triangle => triangle not in cavity
            encroached.add(triangle);
            // Check the neighbors
            for (Triangle neighbor: triGraph.neighbors(triangle)){
                if (marked.contains(neighbor)) continue;
                marked.add(neighbor);
                toBeChecked.add(neighbor);
            }
        }
        return encroached;
    }

    /**
     * Update the triangulation by removing the cavity triangles and then
     * filling the cavity with new triangles.
     * @param site the site that created the cavity
     * @param cavity the triangles with site in their circumcircle
     * @return one of the new triangles
     */
    private Triangle update (Pnt site, Set<Triangle> cavity) {
        Set<Set<Pnt>> boundary = new HashSet<Set<Pnt>>();
        Set<Triangle> theTriangles = new HashSet<Triangle>();

        // Find boundary facets and adjacent triangles
        for (Triangle triangle: cavity) {
            theTriangles.addAll(neighbors(triangle));
            for (Pnt vertex: triangle) {
                Set<Pnt> facet = triangle.facetOpposite(vertex);
                if (boundary.contains(facet)) boundary.remove(facet);
                else boundary.add(facet);
            }
        }
        theTriangles.removeAll(cavity);        // Adj triangles only

        // Remove the cavity triangles from the triangulation
        for (Triangle triangle: cavity) triGraph.remove(triangle);

        // Build each new triangle and add it to the triangulation
        Set<Triangle> newTriangles = new HashSet<Triangle>();
        for (Set<Pnt> vertices: boundary) {
            vertices.add(site);
            Triangle tri = new Triangle(vertices);
            triGraph.add(tri);
            newTriangles.add(tri);
        }

        // Update the graph links for each new triangle
        theTriangles.addAll(newTriangles);    // Adj triangle + new triangles
        for (Triangle triangle: newTriangles)
            for (Triangle other: theTriangles)
                if (triangle.isNeighbor(other))
                    triGraph.add(triangle, other);

        // Return one of the new triangles
        return newTriangles.iterator().next();
    }




    /**
     * Main program; used for testing.
     */
    public static void main (String[] args) {
//        Triangle trit =
//            new Triangle(new Pnt(-10,10), new Pnt(10,10), new Pnt(0,-10));
//        System.out.println("Triangle created: " + trit);
//        Triangulation dt = new Triangulation(trit);
//        System.out.println("DelaunayTriangulation created: " + dt);
//        dt.delaunayPlace(new Pnt(0,0));
//        dt.delaunayPlace(new Pnt(1,0));
//        dt.delaunayPlace(new Pnt(0,1));
//        System.out.println("After adding 3 points, we have a " + dt);
//        Triangle.moreInfo = true;


        //        listS.add(new Pnt(120.177909,30.219316));
//        listS.add(new Pnt(120.184192,30.222278));
//        listS.add(new Pnt(120.174166,30.253418));



//
//
//        //轮廓
//        String str="120.220459, 30.236029;120.203547, 30.248803;120.192586, 30.252134;120.194626, 30.263499;120.164574, 30.264136;120.169216, 30.257224;120.160744, 30.241614;120.166635, 30.236759;120.166718, 30.229765;120.161353, 30.22168;120.16015, 30.223718;120.154024, 30.218562;120.146113, 30.219729;120.150022, 30.211663;120.145279, 30.210434;120.14588, 30.199322;120.17225, 30.205824;120.211475, 30.227183;120.220459, 30.236029";
//        List<Pnt> listSW = Arrays.asList(str.split(";")).stream().map(s->{
//            Double a = Doubles.tryParse(s.split(",")[0].trim());
//            Double b = Doubles.tryParse(s.split(",")[1].trim());
//            return new Pnt(a,b);
//        }) .collect(Collectors.toList());

        String dianStr = "120.177215,30.270029;120.170758,30.26518;120.202104,30.246228;120.177909,30.219316;120.184192,30.222278;120.174166,30.253418;120.180116,30.256563;120.16897,30.250873;120.182955,30.240782;120.135307,30.283257;120.126191,30.294136;120.156922,30.284273;120.111589,30.287802;120.153526,30.274832;120.100924,30.286517;120.104318,30.27162;120.162146,30.281288;120.099976,30.312365;120.135859,30.271052;120.091371,30.169427;120.34354,30.293675;120.340845,30.321908;120.34354,30.293675;120.272338,30.316275;120.35313,30.322716;120.184966,30.270503;120.208275,30.266444;120.180169,30.263245;120.166113,30.274422;120.1733,30.286746;120.16085,30.278084;120.175245,30.271953;120.176366,30.282925;120.17611,30.266684;120.182165,30.263454;120.185832,30.277785;120.188579,30.276179;120.149201,30.32381;120.166039,30.309582;120.129111,30.341422;120.114,30.307147;120.157547,30.292969;120.128831,30.328139;120.213964,30.268787;120.230602,30.26091;120.198458,30.251941;120.175159,30.302803;120.202268,30.265199;120.201009,30.290486;120.21194,30.299091;120.198899,30.233831;120.168645,30.189812;120.21502,30.21434;120.229158,30.214304;120.454496,30.187942;120.273531,30.207305;120.465167,30.187706;120.411053,30.16867;120.338374,30.226042;120.254833,30.05127;120.274644,30.16759;120.266111,30.191444;120.254657,30.17706;120.27667,30.186807;120.264537,30.155583;120.296774,30.209485;120.462677,30.302346;120.281843,30.150456;120.27142,30.186657;120.178453,30.136304;120.290905,30.222739;120.272974,30.175005;120.310971,30.412692;120.300867,30.355263;119.958046,30.257466;119.990181,30.229745;120.192053,30.480762;120.307213,30.431698;120.30732,30.422562;120.305763,30.462095;119.981486,30.396518;119.712943,30.232578;119.828486,30.251204;119.408823,30.186472;119.726625,30.23599;119.710906,30.225078;119.700194,29.796186;119.692064,29.802968;119.629865,29.870977;119.682399,29.812805;119.447346,29.93755;119.958724,30.053764;119.951847,30.061065;119.743709,29.971709;119.967018,30.055039;120.007823,30.012657;119.951623,30.05426;119.956005,30.070142;119.278173,29.478306;119.506264,29.54682;119.23034,29.364132;119.283471,29.478291;119.305784,29.491095;119.298051,29.488752;119.053495,29.609546;119.066595,29.615189";
        List<Pnt> listS = new ArrayList();
        int t = 0;
        for(String strinRR :dianStr.split(";")){
            //点集合
//            System.out.print(t);
            t++;
            listS.add(new Pnt(Doubles.tryParse(strinRR.split(",")[0]),Doubles.tryParse(strinRR.split(",")[1])));
        }

//        listS.addAll(listSW);

        double maxX = 122;
        double minX = 118;
        double maxY = 30;
        double minY = 27.6;


        Triangle trtti = new Triangle(new Pnt(1.5*minX-0.5*maxX,minY), new Pnt(1.5*maxX-0.5*minX,minY), new Pnt(0.5*minX+0.5*maxX,2*maxY-minY));
//        System.out.println("Triangle created: " + trtti);


        Triangulation dt = new Triangulation(trtti);
        System.out.println("DelaunayTriangulation created: " + dt);

        for(int j=0 ; j< listS.size() ; j++){
            dt.delaunayPlace(listS.get(j));
        }
//
//
//        System.out.println("After adding 3 points, we have a " + dt);
//        Triangle.moreInfo = true;
//

        HashSet<Pnt> done = new HashSet<Pnt>(trtti);
        DecimalFormat dcmFmt = new DecimalFormat("0.000000");

        List<List<Map>> lT = new ArrayList();
        for(Triangle triangle :dt){
            for (Pnt site: triangle) {
                if (done.contains(site)) continue;
                done.add(site);
                List<Triangle> list = dt.surroundingTriangles(site, triangle);
                Pnt[] vertices = new Pnt[list.size()];
                int i = 0;
                for (Triangle tri: list){
                    vertices[i++] =tri.getCircumcenter();
                }
                    List<Map> list1 =Arrays.stream(vertices).map(s->{
                        Map map = new HashMap();
                        map.put("lng",new BigDecimal(s.coord(0)).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue());
                        map.put("lat",new BigDecimal(s.coord(1)).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue());
//                        if(d[0] >maxX  ){
//                            map.put("lng",maxX);
//                        }
//                        if(d[0] <minX  ){
//                            map.put("lng",minX);
//                        }
//                        if(d[1] >maxY ){
//                            map.put("lat",maxY);
//                        }
//                        if(d[1] <minY  ){
//                            map.put("lat",minY);
//                        }
                        return map;
                    }).filter(s->s!=null).collect(Collectors.toList());
                        lT.add(list1);
            }
        }
//        int i = 0;
//        for(Pnt pnt :listS){
//            for(List<Map> list1: lT){
//                List<Pnt> list= list1.stream().map(s->new Pnt(Doubles.tryParse(s.get("lng").toString()),Doubles.tryParse(s.get("lat").toString()))).collect(Collectors.toList());
//                try {
//                    if(pnt.isInside(list.toArray(new Pnt[list.size()]))){
//
//                            System.out.println(pnt + " 在 " + JsonUtil.toJsonString(list1));
//                    }
//                }catch (Exception e){
//
//                }
//            }
//            i++;
//        }


//        List cilp = listSW.stream().map(s-> new HashMap(){{put("lng",s.getCoordinates()[0]);put("lat",s.getCoordinates()[1]);}}).collect(Collectors.toList());
//        int i = 0;
//        for(List<Map> listF:lT){
//            i++;
//            WeilerAtherton weilerAtherton = new WeilerAtherton();
//            weilerAtherton.WeilerAthertonClipT(listF,cilp,i);
//        }

//        System.out.print(JsonUtil.toJsonString(lT));
        System.out.println(JsonUtil.toJsonString(lT));
        System.out.println("    ");


    }
}