package com.example.demo.voronoi;//package com.yscredit.util.voronoi;
//
//import com.google.common.primitives.Doubles;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * Created by YS on 2019/3/5.
// */
//public class VoronoiUtil {
//
//    public static void main(String[] args) {
//        List list = new ArrayList();
//        String str="120.220459, 30.236029;120.203547, 30.248803;120.192586, 30.252134;120.194626, 30.263499;120.164574, 30.264136;120.169216, 30.257224;120.160744, 30.241614;120.166635, 30.236759;120.166718, 30.229765;120.161353, 30.22168;120.16015, 30.223718;120.154024, 30.218562;120.146113, 30.219729;120.150022, 30.211663;120.145279, 30.210434;120.14588, 30.199322;120.17225, 30.205824;120.211475, 30.227183;120.220459, 30.236029";
//        list = Arrays.asList(str.split(";")).stream().map(s->{
//            Double a = Doubles.tryParse(s.split(",")[0].trim());
//            Double b = Doubles.tryParse(s.split(",")[1].trim());
//            return new Pnt(a,b);
//        }) .collect(Collectors.toList());
//        Triangle tri = new Triangle(list);
//        System.out.println("Triangle created: " + tri);
//        Triangulation dt = new Triangulation(tri);
//        System.out.println("DelaunayTriangulation created: " + dt);
//        dt.delaunayPlace(new Pnt(120.177909,30.219316));
//        dt.delaunayPlace(new Pnt(120.184192,30.222278));
//        dt.delaunayPlace(new Pnt(120.174166,30.253418));
//        dt.delaunayPlace(new Pnt(120.180116,30.256563));
//        dt.delaunayPlace(new Pnt(120.16897,30.250873));
//        dt.delaunayPlace(new Pnt(120.182955,30.240782));
//        dt.delaunayPlace(new Pnt(120.180169,30.263245));
//        dt.delaunayPlace(new Pnt(120.182165,30.263454));
//        dt.delaunayPlace(new Pnt(120.198899,30.233831));
//        System.out.println("After adding 3 points, we have a " + dt);
//        Triangle.moreInfo = true;
//        System.out.println("Triangles: " + dt.getNodeKeySet());
//
//    }
//}
