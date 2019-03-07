package com.example.demo.voronoi;


import com.example.demo.util.JsonUtil;

import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class WeilerAtherton {



//	List<Polygon> subject;	// 主多边形
//	Polygon clipping;	// 裁剪多边形。只能有一个裁剪多边形，所以不用list存储
	int drawStatus;




	public WeilerAtherton() {
		// 类构造方法
		drawStatus = 0;
		Frame f = new Frame();
	}

	public List<Polygon> WeilerAthertonClip(List<Polygon> subject ,Polygon clipping,boolean inside) {
		// 核心算法

		// 输入参数决定是内裁减还是外裁减
		// 内外裁剪唯一的区别就是连顶点时是顺时针还是逆时针

		// 裁减多边形和主多边形的点存储顺序都要改成顺时针
		// 这个会影响到内裁减和外裁减按什么方向连接顶点
		if (!clipping.isClockwise()) {
			// System.out.println("clipping not clockwise!");
			clipping.reverse();
		}
		Iterator polyItr = subject.iterator();
		Polygon polyPtr = null;
		while (polyItr.hasNext()) {
			polyPtr = (Polygon)polyItr.next();
			if (!polyPtr.isClockwise()) {
//				System.out.println("subject not clockwise!");
				polyPtr.reverse();
			}
		}

		// 根据多边形建立两个顶点表
		PointTable pointTable = new PointTable(subject,clipping);

		// 表建立完毕，开始产生新多边形
		// 也就是根据两个顶点表，按某个顺序连接其中的点
		// 具体就不说了，看看Weiler-Atherton算法就明白了
		// 重点在于判断点是入点（entering intersection）还是出点（exiting intersection）
		boolean alltraced = false;
		int traceSize = pointTable.clippingPoint.size();
		Point tempPoint;
		Iterator clippingItr = pointTable.clippingPoint.iterator();

		boolean inPoint = true;
		Iterator itr1, itr2;
		Point tempP1, tempP2, startPoint;
		Polygon newPoly;
		List<Polygon> polyList = new LinkedList();

		while (true) {
			// ++i;
			newPoly = new Polygon();

			// itr1用于寻找第一个未追踪的交点，itr2用于记录它前一个点
			itr1 = pointTable.subjectPoint.iterator();
			itr2 = itr1;
			tempP1 = (Point)itr1.next();	// 第一个点不会是交点，因此可以让tempP1在循环一开始的时候指向第二个点
			tempP2 = tempP1;

			alltraced = true;
			while (itr1.hasNext()) {
				tempP2 = tempP1;
				tempP1 = (Point)itr1.next();
				if (tempP1.type == 1) {
					alltraced = false;
					break;
				}
			}
			if (alltraced) {
				break;
			}

			// 找到新的未追踪交点tempP1

//			System.out.print("------look for untraced point this round------");
//			tempP1.printlnPoint();

			// 利用前一个点tempP2判断交点是出点还是入点
			if (clipping.pointInside(tempP2)) {
//				System.out.println("an out point.");
				inPoint = false;
			}
			else {
//				System.out.println("an in point.");
				inPoint = true;
			}

			startPoint = tempP1;
			if (inPoint == inside) {
				// 先找主多边形
				while (true) {
					// 在主多边形表里
//					System.out.print("looking for point in subject");
//					System.out.print("	point is:");
//					tempP1.printlnPoint();

					itr2 = pointTable.findFirstInSubject(tempP1);
					if (itr2 == null) {
						// 如果找不到该点，说明已经回到起点
//						System.out.println("cannot find. breaking.");
						break;
					}
//					System.out.println("found in subject.");
					// tempP2 = (Point)itr2.next();

					while (true) {
						if (pointTable.subjectAllTraced() && tempP2 == startPoint) {
							break;
						}
						if (!itr2.hasNext()) {
							itr2 = pointTable.subjectPoint.iterator();
							itr2.next();
						}
						tempP2 = (Point)itr2.next();

						if (tempP2 == tempP1) {
							tempP2.type = 0;
							newPoly.p.add(new Point(tempP2));
//							System.out.print("added:");
//							tempP2.printlnPoint();
						}
						else if (tempP2.type == 0) {
							newPoly.p.add(new Point(tempP2));

//							System.out.print("point added:");
//							tempP2.printlnPoint();
						}
						else {
//							System.out.print("meet new:");
//							tempP2.printlnPoint();
							break;
						}
					}


//					System.out.print("looking for point in clipping :");
//					tempP2.printlnPoint();

					// 在裁减多边形表里
					itr1 = pointTable.findFirstInClipping(tempP2);
					if (itr1 == null) {
						// 如果找不到该点，说明已经回到起点
//						System.out.println("cannot find in clipping. breaking.");
						break;
					}
					// tempP1 = (Point)itr1.next();

					while (true) {
						if (pointTable.clippingAllTraced() && tempP1 == startPoint) {
							break;
						}
						if (!itr1.hasNext()) {
							itr1 = pointTable.clippingPoint.iterator();
							itr1.next();
						}
						tempP1 = (Point)itr1.next();

						if (tempP1 == tempP2) {
							tempP1.type = 0;
							newPoly.p.add(new Point(tempP1));
						}
						else if (tempP1.type == 0) {
							newPoly.p.add(new Point(tempP1));

//							System.out.print("point added in clipping:");
//							tempP1.printlnPoint();
						}
						else {
							break;
						}
					}

//					System.out.println("round over");
				}
			} else {
				while (true) {
//					System.out.print("looking for out point in clipping");
//					System.out.print("	point is:");
//					tempP1.printlnPoint();
//					System.out.println("	added.");
					// 先找裁剪多边形

					itr1 = pointTable.findFirstInClipping(tempP1);
					if (itr1 == null) {
						// 如果找不到该点，说明已经回到起点
//						System.out.println("cannot find. Over1.");
						break;
					}
					// itr1.next();

					while (true) {
						if (pointTable.clippingAllTraced() && tempP2 == startPoint) {
							break;
						}
						if (!itr1.hasNext()) {
							itr1 = pointTable.clippingPoint.iterator();
							itr1.next();
						}
						tempP2 = (Point)itr1.next();

						if (tempP2 == tempP1) {
							tempP2.type = 0;
							newPoly.p.add(new Point(tempP2));
						}
						else if (tempP2.type == 0) {
							newPoly.p.add(new Point(tempP2));

//							System.out.print("point added:");
//							tempP2.printlnPoint();
						}
						else {
							break;
						}
					}

					itr2 = pointTable.findFirstInSubject(tempP2);
					if (itr2 == null) {
						// 如果找不到该点，说明已经回到起点
//						System.out.println("cannot find. Over2.");
						break;
					}
					// tempP1 = (Point)itr2.next();

					while (true) {
						if (pointTable.subjectAllTraced() && tempP1 == startPoint) {
							break;
						}
						if (!itr2.hasNext()) {
							itr2 = pointTable.subjectPoint.iterator();
							itr2.next();
						}
						tempP1 = (Point)itr2.next();

						if (tempP1 == tempP2) {
							tempP1.type = 0;
							newPoly.p.add(new Point(tempP1));

//							System.out.print("added this:");
//							tempP1.printlnPoint();
						}
						else if (tempP1.type == 0) {
							newPoly.p.add(new Point(tempP1));

//							System.out.print("point added:");
//							tempP1.printlnPoint();
						}
						else {
							break;
						}
					}

					// 在裁减多边形表里找点
					itr1 = pointTable.findFirstInClipping(tempP1);
					if (itr1 == null) {
						// 如果找不到该点，说明已经回到起点
//						System.out.println("cannot find. Over3.");
						break;
					}
				}
			}
			tempP1 = newPoly.p.get(0);
			newPoly.p.add(new Point(tempP1));
			newPoly.done = true;
			polyList.add(newPoly);
		}
		return polyList;

	}

	class Point{
		// 二维点
		// type表示该点是内点还是外点

		double x, y ;
		Integer type;

		public Point() {
			x = y = type = 0;
		}

		public Point(double _x, double _y) {
			x = _x;
			y = _y;
			type = 0;
		}

		public Point(double _x, double _y, int _type) {
			x = _x;
			y = _y;
			type = _type;
		}

		public Point(Point b) {
			this.x = b.x;
			this.y = b.y;
			this.type = b.type;
		}

		public void printPoint(){
			System.out.print("(");
			System.out.print(x);
			System.out.print(", ");
			System.out.print(y);
			System.out.print(", ");
			System.out.print(type);
			System.out.print(")");
		}

		public void printlnPoint(){
			System.out.print("(");
			System.out.print(x);
			System.out.print(", ");
			System.out.print(y);
			System.out.print(", ");
			System.out.print(type);
			System.out.println(")");
		}

		public double distanceSquareTo(Point another) {
			// 计算到另一个点距离的平方
			return (x - another.x) * (x - another.x) + (y - another.y) * (y - another.y);
		}

	}

	class Polygon{
		// 多边形类
		List<Point> p;
		boolean done;

		public Polygon() {
			p = new LinkedList<Point>();
			done = false;
		}

		public boolean isClockwise() {
			// 判断其存储的点是按顺时针还是逆时针顺序
			int maxY = 0;
			Iterator itr = p.iterator();
			Point temp1, temp2, temp3, maxYPoint = new Point(0, 0);

			while (itr.hasNext()) {
				temp1 = (Point)itr.next();
				if (temp1.y > maxY) {
					maxYPoint = temp1;
				}
			}

			itr = p.iterator();
			temp2 = (Point)itr.next();
			if (temp2 == maxYPoint) {
				temp3 = (Point)itr.next();
				temp1 = p.get(p.size() - 2);
			}
			else {
				temp1 = temp2;
				while (itr.hasNext()) {
					temp2 = (Point)itr.next();
					if (temp2 == maxYPoint) {
						break;
					}
					else {
						temp1 = temp2;
					}
				}
				if (itr.hasNext()) {
					temp3 = (Point)itr.next();
				}
				else {
					temp3 = p.get(1);
				}
			}
			/*
			temp1.printlnPoint();
			temp2.printlnPoint();
			temp3.printlnPoint();
			*/
			Point v0 = new Point(temp2.x - temp1.x, temp2.y - temp1.y);
			Point v1 = new Point(temp3.x - temp2.x, temp3.y - temp2.y);
			if (v0.x * v1.y - v0.y * v1.x > 0) {
				return false;
			}
			else {
				return true;
			}
		}

		private void reverse() {
			// 这个函数用于将多边形的点顺序反转
			// 即如果原多边形是按逆时针顺序存储点，则改为顺时针顺序
			Point[] ps = new Point[p.size()];
			Iterator itr = p.iterator();
			int size = p.size();
			int i = size - 1;
			while (itr.hasNext()) {
				ps[i] = new Point((Point)itr.next());
				--i;
			}

			p = new LinkedList<Point>();
			i = 0;
			while (i < size) {
				p.add(new Point(ps[i]));
				++i;
			}
		}

		public boolean pointInside(Point a) {
			// 判断某点是否在多边形内
			Iterator itr = p.iterator();

			Point cur1 = (Point)itr.next();
			Point cur2;
			double x;
			int count = 0;
			while (itr.hasNext()) {
				cur2 = (Point)itr.next();
				if (cur1.y == cur2.y && cur1.y == a.y) {
					continue;
				}
				if (a.y < cur1.y && a.y < cur2.y) {
					continue;
				}
				if (a.y >= cur1.y && a.y >= cur2.y) {
					continue;
				}

				x = (a.y - cur1.y) * (cur2.x - cur1.x) / (cur2.y - cur1.y) + cur1.x;
				if (x > a.x) {
					++count;
				}

				cur1 = cur2;
			}

			if (count % 2 == 1) {
				return true;
			}
			else {
				return false;
			}
		}

	}

	class PointTable{
		// 顶点表
		// 有一个主多边形顶点表和一个裁剪多边形顶点表
		// 算法需要用，不明白的回去看算法

		List<Point> subjectPoint, clippingPoint;

		public PointTable(List<Polygon> subject,Polygon clipping) {
			subjectPoint = new LinkedList<Point>();
			clippingPoint = new LinkedList<Point>();

			Iterator itr = subject.iterator();
			while (itr.hasNext()) {
				addToSubject((Polygon)itr.next());
			}
			addToClipping(clipping);

			findCrossing(subject,clipping);
		}

	public void addToSubject(Polygon poly) {
		// 将多边形顶点加入主多边形顶点表
		Iterator itr = poly.p.iterator();
		while (itr.hasNext()) {
			subjectPoint.add(new Point((Point)(itr.next())));
		}
	}

	public void addToClipping(Polygon poly) {
		// 加入裁剪多边形顶点表
		Iterator itr = poly.p.iterator();
		while (itr.hasNext()) {
			clippingPoint.add(new Point((Point)(itr.next())));
		}
	}

	public boolean subjectAllTraced() {
		// 判断是否已经将主多边形的全部点连过一遍
		// 这个是用来画最后裁剪出来的形状的，如果主多边形表全部连过，说明裁剪成功的图片已画完
		Iterator itr = subjectPoint.iterator();
		Point temp = new Point(0, 0);
		while (itr.hasNext()) {
			temp = (Point)itr.next();
			if (temp.type != 0) {
				return false;
			}
		}
		return true;
	}

	public boolean clippingAllTraced() {
		// 判断裁减多边形是否全部连过一遍
		Iterator itr = clippingPoint.iterator();
		Point temp = new Point(0, 0);
		while (itr.hasNext()) {
			temp = (Point)itr.next();
			if (temp.type != 0) {
				return false;
			}
		}
		return true;
	}

	public Iterator findFirstInSubject(Point a) {
		// 在主多边形里找到该点，返回iterator
//		System.out.println("***looking for***");
//		a.printlnPoint();

		ListIterator itr = subjectPoint.listIterator();
		Iterator itr_pre = subjectPoint.iterator();
		Point temp;
		while (itr.hasNext()) {
			temp = (Point)itr.next();
			if (temp.type != 0 && temp.x == a.x && temp.y == a.y) {
				break;
			}
			itr_pre.next();
		}


		if (itr.hasNext()) {
//			System.out.println("***done***");
			return itr_pre;
		}
		else {
//			System.out.println("***not found!!!!!!!!!***");
			return null;
		}
	}

	public Iterator findFirstInClipping(Point a) {
		// 在裁减多边形里找点，返回iterator
//		System.out.println("***looking for in clipping***");
//		a.printlnPoint();

		ListIterator itr = clippingPoint.listIterator();
		Iterator itr_pre = clippingPoint.iterator();
		Point temp;
		while (itr.hasNext()) {
			temp = (Point)itr.next();
			if (temp.type != 0 && temp.x == a.x && temp.y == a.y) {
				break;
			}
			itr_pre.next();
		}

		if (itr.hasNext()) {
//			System.out.println("***done***");
			return itr_pre;
		}
		else {
//			System.out.println("***not found!!!!!!!!!***");
			return null;
		}
	}

	public void findCrossing(List<Polygon> subject,Polygon clipping) {
		// 找到主多边形和裁减多边形所有的交点！

		Iterator clippingPointItr = clipping.p.iterator();
		Iterator subjectPointItr;
		Iterator subjectItr = subject.iterator();

		Point p1 = (Point)clippingPointItr.next();
		Point p2, p3, p4, crossing;
		Polygon currentPoly;
		while (clippingPointItr.hasNext()) {
			p2 = (Point)clippingPointItr.next();

			// find crossing of this segment p1p2;
			subjectItr = subject.iterator();
			while (subjectItr.hasNext()) {
				// do below to all the subject polygons

				currentPoly = (Polygon)subjectItr.next();

				subjectPointItr = currentPoly.p.iterator();
				p3 = (Point)subjectPointItr.next();
				while (subjectPointItr.hasNext()) {
					p4 = (Point)subjectPointItr.next();

//					System.out.print("Checking segment p1 ");
//					p1.printPoint();
//					System.out.print(" and p2 ");
//					p2.printPoint();
//					System.out.print("with segment p3 ");
//					p3.printPoint();
//					System.out.print(" and p4 ");
//					p4.printPoint();
//					System.out.println();

					// p1, p2 is one segment in clipping while p3, p4 is a segment in current poly
					if (lineCrossing(p1, p2, p3, p4)) {
//						System.out.print("crossing...\t");
						crossing = getCrossingPoint(p1, p2, p3, p4);
						if (crossing != null) {
							// System.out.print("not null...\n");
//							crossing.printlnPoint();
							//insertToSubject(crossing, subjectItr, subjectPointItr, subjectPointItr2);
							insertToSubject(crossing, p3, p4);
							//insertToClipping(crossing, clippingPointItr, clippingPointItr2);
							insertToClipping(crossing, p1, p2);
						}
					}
					p3 = p4;
				}
			}

			p1 = p2;
		}
			/*
			//System.out.println("------subject points------");
			Iterator itr = subjectPoint.iterator();
			while (itr.hasNext()) {
				((Point)itr.next()).printPoint();
			}
			//System.out.println("\n-----------over-----------");
			//System.out.println("------clipping points------");
			itr = clippingPoint.iterator();
			while (itr.hasNext()) {
				((Point)itr.next()).printPoint();
			}
			//System.out.println("\n-----------over-----------");
			 */
	}

	public void insertToSubject(Point crossing, Point p1, Point p2) {
		// 将交点插入主多边形当中的正确位置
		// 需要插到主多边形中两个顶点之间
		// 一定要考虑如果主多边形一条边上有两个交点，怎么排序

		Iterator subjectItr = subjectPoint.iterator();
		int i = 0;
		Point curPoint;

		while (subjectItr.hasNext()) {
			curPoint = (Point)subjectItr.next();
			++i;
			if (curPoint.x == p1.x && curPoint.y == p1.y) {
				break;
			}
		}

		while (subjectItr.hasNext()) {
			curPoint = (Point)subjectItr.next();
			if (curPoint.x == p2.x && curPoint.y == p2.y) {
				break;
			}
			else if (p1.distanceSquareTo(crossing) <= p1.distanceSquareTo(curPoint)) {
				break;
			}
			else {
				++i;
			}
		}

		subjectPoint.add(i, crossing);
	}

	public void insertToClipping(Point crossing, Point p1, Point p2) {
		// 交点插入裁剪多边形

		Iterator clippingItr = clippingPoint.iterator();
		int i = 0;
		Point curPoint;

		while (clippingItr.hasNext()) {
			curPoint = (Point)clippingItr.next();
			++i;
			if (curPoint.x == p1.x && curPoint.y == p1.y) {
				break;
			}
		}

		while (clippingItr.hasNext()) {
			curPoint = (Point)clippingItr.next();
			if (curPoint.x == p2.x && curPoint.y == p2.y) {
				break;
			}
			else if (p1.distanceSquareTo(crossing) <= p1.distanceSquareTo(curPoint)) {
				break;
			}
			else {
				++i;
			}
		}

		clippingPoint.add(i, crossing);
	}

	private final double min(double a, double b) {
		return (a < b) ? a : b;
	}

	private final double max(double a, double b) {
		return (a > b) ? a : b;
	}

	private boolean lineCrossing(Point a, Point b, Point c, Point d) {
		// 判断两条线是否有交点
		// 这个算法是网上抄的，不理解可以去搜搜

		// 先做快速排斥：
		if (!(min(a.x, b.x) <= max(c.x, d.x)
				&& min(c.y, d.y) <= max(a.y, b.y)
				&& min(c.x, d.x) <= max(a.x, b.x)
				&& min(a.y,b.y)<=max(c.y,d.y))
				) {
			return false;
		}

		// 通过快速排斥实验后，做跨立实验：
		double u, v, w, z;	//分别记录两个向量
		u = (c.x - a.x) * (b.y - a.y) - (b.x - a.x) * (c.y - a.y);
		v = (d.x - a.x) * (b.y - a.y) - (b.x - a.x) * (d.y - a.y);
		w = (a.x - c.x) * (d.y - c.y) - (d.x - c.x) * (a.y - c.y);
		z = (b.x - c.x) * (d.y - c.y) - (d.x - c.x) * (b.y - c.y);

		if ((double)u / v <= 0 && (double)w / z <= 0) {
			return true;
		}
		else {
			return false;
		}
	}

	private Point getCrossingPoint(Point a, Point b, Point c, Point d) {
		// 计算两条线的交点

		Point ab = new Point(b.x - a.x, b.y - a.y);
		Point cd = new Point(d.x - c.x, d.y - c.y);
		if ((double)ab.x / (double)cd.x == (double)ab.y / (double)cd.y) {
			System.out.print("null!");
//			ab.printPoint();
//			cd.printPoint();
			System.out.println();
			return null;
		}

		double tmpLeft,tmpRight;
		// tmpLeft = (d.x - c.x) * (a.y - b.y) - (b.x - a.x) * (c.y - d.y);
		tmpLeft = ab.x * cd.y - cd.x * ab.y;
		// tmpRight = (a.y - c.y) * (b.x - a.x) * (d.x - c.x) + c.x * (d.y - c.y) * (b.x - a.x) - a.x * (b.y - a.y) * (d.x - c.x);
		tmpRight = (a.y - c.y) * ab.x * cd.x + c.x * cd.y * ab.x - a.x * ab.y * cd.x;
		double xout = ((double)tmpRight/(double)tmpLeft);

		// tmpLeft = (a.x - b.x) * (d.y - c.y) - (b.y - a.y) * (c.x - d.x);
		tmpLeft = ab.y * cd.x - ab.x * cd.y;
		// tmpRight = b.y * (a.x - b.x) * (d.y - c.y) + (d.x- b.x) * (d.y - c.y) * (a.y - b.y) - d.y * (c.x - d.x) * (b.y - a.y);
		tmpRight = b.y * -1 * ab.x * cd.y + (d.x- b.x) * cd.y * -1 * ab.y - d.y * -1 * cd.x * ab.y;
		double yout = ((double)tmpRight/(double)tmpLeft);
		return new Point(xout, yout, 1);
	}

}

	public void WeilerAthertonClipT(List<Map> graph ,List<Map> clipGragh,int i) {
		try {
			Polygon currentPoly = new Polygon();
			graph.stream().forEach(e -> currentPoly.p.add(new Point(Double.parseDouble(e.get("lng").toString()), Double.parseDouble(e.get("lat").toString()))));

			Polygon clipping = new Polygon();
			clipGragh.stream().forEach(e -> clipping.p.add(new Point(Double.parseDouble(e.get("lng").toString()), Double.parseDouble(e.get("lat").toString()))));
			WeilerAtherton weilerAthertonClip = new WeilerAtherton();
			List<Polygon> list = weilerAthertonClip.WeilerAthertonClip(new ArrayList() {{add(currentPoly);}}, clipping, true);
			if(list.get(0).p.size() >= 3) {
//				weilerAthertonClip = new WeilerAtherton();
//				list = weilerAthertonClip.WeilerAthertonClip(new ArrayList() {{add(currentPoly);}}, clipping, true);


				Polygon polygon = list.get(0);

				List listX = polygon.p.stream().map(s -> new HashMap() {{
					put("lng", new BigDecimal(s.x).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue());
					put("lat", new BigDecimal(s.y).setScale(6, BigDecimal.ROUND_HALF_UP).doubleValue());
				}}).collect(Collectors.toList());
				System.out.println("," + JsonUtil.toJsonString(listX));
			}else {
				System.out.println("," + JsonUtil.toJsonString(graph));
			}




		}catch (Exception e){
		e.printStackTrace();
		}
	}



//	final int CANVAS_WIDTH = 1024;
//	final int CANVAS_HEIGHT = 720;
//	final int UI_HEIGHT = 120;
//	final int BUTTON_WIDTH =150;
//
//	CanvasPanel canvas = new CanvasPanel();
//	UIPanel u = new UIPanel();
//	public static void main(String[] args) {
//		WeilerAtherton wa = new WeilerAtherton();
//	}
//
//	class Frame extends JFrame{
//		// 做图形界面的Frame
//
//		public Frame() {
//			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			this.setSize(CANVAS_WIDTH, CANVAS_HEIGHT + UI_HEIGHT);
//			this.setLayout(new BorderLayout());
//
//			UIPanel uip = u;
//			CanvasPanel cp = canvas;
//			this.add(uip, BorderLayout.NORTH);
//			this.add(cp, BorderLayout.CENTER);
//
//			this.setVisible(true);
//		}
//
//	}
//
//	class UIPanel extends JPanel{
//		// Frame里面的面板
//
//		JLabel label;
//		JPanel buttonPanel;
//		JButton drawSubject, drawClipping, insideClip, outsideClip, clear;
//
//		JTextField mx = new JTextField();
//		JTextField my = new JTextField();
//
//		public UIPanel() {
//
//			this.setPreferredSize(new Dimension(CANVAS_WIDTH, UI_HEIGHT));
//
//			this.setLayout(new BorderLayout());
//
//			label = new JLabel();
//			buttonPanel = new JPanel();
//
//			drawSubject = new JButton("绘制主多边形");
//			drawClipping = new JButton("绘制裁剪多边形");
//			insideClip = new JButton("内裁减");
//			outsideClip = new JButton("外裁减");
//			clear = new JButton("清空");
//
//			drawSubject.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					canvas.currentPoly = new Polygon();
//					canvas.repaint();
//					drawStatus = 0;
//				}
//			});
//
//			drawClipping.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					canvas.currentPoly = new Polygon();
//					canvas.repaint();
//					drawStatus = 1;
//				}
//			});
//
//			insideClip.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					if (clipping.p.isEmpty() || !clipping.done || subject.isEmpty()) {
//						clipping = new Polygon();
//						subject = new LinkedList<Polygon>();
//						return;
//					}
//					canvas.currentPoly = new Polygon();
//					canvas.repaint();
//					WeilerAthertonClip(true);
//				}
//			});
//
//			outsideClip.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed (ActionEvent e) {
//					if (clipping.p.isEmpty() || !clipping.done || subject.isEmpty()) {
//						clipping = new Polygon();
//						subject = new LinkedList<Polygon>();
//						return;
//					}
//					canvas.currentPoly = new Polygon();
//					canvas.repaint();
//					WeilerAthertonClip(false);
//				}
//			});
//
//			clear.addActionListener(new ActionListener() {
//				@Override
//				public void actionPerformed (ActionEvent e) {
//					canvas.currentPoly = new Polygon();
//					subject = new LinkedList<Polygon>();		// 这样做会造成内存泄漏吗？存疑
//					clipping = new Polygon();
//
//					canvas.repaint();
//				}
//			});
//
//			label.setPreferredSize(new Dimension(CANVAS_WIDTH - BUTTON_WIDTH, UI_HEIGHT));
//			label.setText("点击左键绘制多边形，点击右键完成当前多边形（自动闭合）");
//			buttonPanel.setPreferredSize(new Dimension(BUTTON_WIDTH, UI_HEIGHT));
//			buttonPanel.setLayout(new GridLayout(7, 1));
//
//			buttonPanel.add(drawSubject, BorderLayout.NORTH);
//			buttonPanel.add(drawClipping, BorderLayout.SOUTH);
//			buttonPanel.add(insideClip, BorderLayout.SOUTH);
//			buttonPanel.add(outsideClip, BorderLayout.SOUTH);
//			buttonPanel.add(clear, BorderLayout.SOUTH);
//
//
//			buttonPanel.add(mx);
//			buttonPanel.add(my);
//
//			this.add(label, BorderLayout.WEST);
//			this.add(buttonPanel, BorderLayout.CENTER);
//
//		}
//
//	}
//
//	class CanvasPanel extends JPanel {
//
//		Polygon currentPoly;
//
//		public CanvasPanel() {
//			currentPoly = new Polygon();
//
//			this.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
//			this.setBackground(Color.WHITE);
//
//			this.addMouseListener(new MouseListener() {
//				@Override
//				public void mouseClicked(MouseEvent e) {
//					if (e.getButton() == MouseEvent.BUTTON1) {
//						if (drawStatus == 0) {
//							currentPoly.p.add(new Point(e.getX(), e.getY()));
//						}
//						else if (drawStatus == 1) {
//							if (!clipping.done) {
//								clipping.p.add(new Point(e.getX(), e.getY()));
//							}
//						}
//					}
//					if (e.getButton() == MouseEvent.BUTTON3) {
//						if (drawStatus == 0) {
//							currentPoly.done = true;
//							currentPoly.p.add(new Point(currentPoly.p.get(0).x, currentPoly.p.get(0).y));
//							subject.add(currentPoly);
//							currentPoly = new Polygon();
//						}
//						else if (drawStatus == 1) {
//							clipping.done = true;
//							clipping.p.add(new Point(clipping.p.get(0).x, clipping.p.get(0).y));
//						}
//					}
//					repaint();
//				}
//				@Override
//				public void mouseEntered(MouseEvent e) {
//				}
//				@Override
//				public void mouseExited(MouseEvent e) {
//				}
//				@Override
//				public void mousePressed(MouseEvent e) {
//				}
//				@Override
//				public void mouseReleased(MouseEvent e) {
//				}
//			});
//
//			this.addMouseMotionListener(new MouseMotionListener(){
//				@Override
//				public void mouseDragged(MouseEvent e){
//				}
//
//				@Override
//				public void mouseMoved(MouseEvent e){
//					u.mx.setText(String.valueOf(e.getX()));
//					u.my.setText(String.valueOf(e.getY()));
//				}
//			});
//		}
//
//		@Override
//		public void paintComponent(Graphics g) {
//			super.paintComponent(g);
//
//			currentPoly.paintSelf(g);
//			g.setColor(Color.BLACK);
//
//			Iterator itr = subject.iterator();
//			while (itr.hasNext()) {
//				((Polygon)itr.next()).paintSelf(g);
//			}
//
//			g.setColor(Color.RED);
//			clipping.paintSelf(g);
//			g.setColor(Color.BLACK);
//		}
//	}
}
