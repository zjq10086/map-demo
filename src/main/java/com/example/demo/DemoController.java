package com.example.demo;

import com.example.demo.voronoi.Pnt;
import com.example.demo.voronoi.Triangle;
import com.example.demo.voronoi.Triangulation;
import com.google.common.primitives.Doubles;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * demo
 * Created by zjq on 2019/3/7.
 */
@Controller
public class DemoController {
    @RequestMapping("/index")
    @ResponseBody
    public String index(){
        return "index";
    }

    @RequestMapping("/")
    public String demo(){
        return "demo";
    }

    @RequestMapping("getData")
    @ResponseBody
    public List<List<Map>> demo(String dianStr){

//        String dianStr = "120.177215,30.270029;120.170758,30.26518;120.202104,30.246228;120.177909,30.219316;120.184192,30.222278;120.174166,30.253418;120.180116,30.256563;120.16897,30.250873;120.182955,30.240782;120.135307,30.283257;120.126191,30.294136;120.156922,30.284273;120.111589,30.287802;120.153526,30.274832;120.100924,30.286517;120.104318,30.27162;120.162146,30.281288;120.099976,30.312365;120.135859,30.271052;120.091371,30.169427;120.34354,30.293675;120.340845,30.321908;120.34354,30.293675;120.272338,30.316275;120.35313,30.322716;120.184966,30.270503;120.208275,30.266444;120.180169,30.263245;120.166113,30.274422;120.1733,30.286746;120.16085,30.278084;120.175245,30.271953;120.176366,30.282925;120.17611,30.266684;120.182165,30.263454;120.185832,30.277785;120.188579,30.276179;120.149201,30.32381;120.166039,30.309582;120.129111,30.341422;120.114,30.307147;120.157547,30.292969;120.128831,30.328139;120.213964,30.268787;120.230602,30.26091;120.198458,30.251941;120.175159,30.302803;120.202268,30.265199;120.201009,30.290486;120.21194,30.299091;120.198899,30.233831;120.168645,30.189812;120.21502,30.21434;120.229158,30.214304;120.454496,30.187942;120.273531,30.207305;120.465167,30.187706;120.411053,30.16867;120.338374,30.226042;120.254833,30.05127;120.274644,30.16759;120.266111,30.191444;120.254657,30.17706;120.27667,30.186807;120.264537,30.155583;120.296774,30.209485;120.462677,30.302346;120.281843,30.150456;120.27142,30.186657;120.178453,30.136304;120.290905,30.222739;120.272974,30.175005;120.310971,30.412692;120.300867,30.355263;119.958046,30.257466;119.990181,30.229745;120.192053,30.480762;120.307213,30.431698;120.30732,30.422562;120.305763,30.462095;119.981486,30.396518;119.712943,30.232578;119.828486,30.251204;119.408823,30.186472;119.726625,30.23599;119.710906,30.225078;119.700194,29.796186;119.692064,29.802968;119.629865,29.870977;119.682399,29.812805;119.447346,29.93755;119.958724,30.053764;119.951847,30.061065;119.743709,29.971709;119.967018,30.055039;120.007823,30.012657;119.951623,30.05426;119.956005,30.070142;119.278173,29.478306;119.506264,29.54682;119.23034,29.364132;119.283471,29.478291;119.305784,29.491095;119.298051,29.488752;119.053495,29.609546;119.066595,29.615189";
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
                List<Map> list1 = Arrays.stream(vertices).map(s->{
                    Map map = new HashMap();
                    map.put("lng",new BigDecimal(s.coord(0)).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue());
                    map.put("lat",new BigDecimal(s.coord(1)).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue());
                    return map;
                }).filter(s->s!=null).collect(Collectors.toList());
                lT.add(list1);
            }
        }

        return lT;
    }

}
