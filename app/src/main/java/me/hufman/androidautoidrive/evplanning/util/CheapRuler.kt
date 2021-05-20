// CheapRuler.kt is licensed under the BSD 3-Clause
// License, https://opensource.org/licenses/BSD-3-Clause
//
// Copyright (c) 2020, Ian Emmons
// Copyright (c) 2021, Norbert Truchsess. All rights reserved.
package me.hufman.androidautoidrive.evplanning.util

import kotlin.math.*

/**
 * A collection of very fast approximations to common geodesic measurements.
 * Useful for performance-sensitive code that measures things on a city scale.
 * Can be an order of magnitude faster than corresponding
 * [Turf](http://turfjs.org/) methods.
 *
 * The approximations are based on the
 * [WGS84
 * ellipsoid model of the Earth](https://en.wikipedia.org/wiki/Earth_radius#Meridional), projecting coordinates to a flat surface
 * that approximates the ellipsoid around a certain latitude. For distances
 * under 500 kilometers and not on the poles, the results are very precise
 *  within 0.1% margin of error compared to
 * [Vincenti](https://en.wikipedia.org/wiki/Vincenty%27s_formulae)
 * formulas, and usually much less for shorter distances.
 *
 * The CheapRuler class is immutable.
 *
 * This library is a A Kotlin port of the original
 * [JavaScript library](https://github.com/mapbox/cheap-ruler).
 * it is based on the Java Port done by Ian Emmons
 * [Java library]https://github.com/IanEmmons/cheap-ruler-java
 */

/** An enumeration of distance units supported by CheapRuler.  */
enum class Units(
		/**
		 * Returns the factor by which one must multiply a distance in kilometers to
		 * convert it to a distance in this unit.
		 *
		 * @return Conversion factor from kilometers to this unit
		 */
		val multiplier: Double) {
	KILOMETERS(1.0), MILES(1000.0 / 1609.344), NAUTICAL_MILES(1000.0 / 1852.0), METERS(1000.0), METRES(1000.0), YARDS(1000.0 / 0.9144), FEET(1000.0 / 0.3048), INCHES(1000.0 / 0.0254)
}

data class Point(val lat: Double, val lon: Double)
data class Box(val min: Point, val max: Point)
data class PointIndexT internal constructor(val point: Point, val index: Int, val t: Double)
typealias LinearRing = List<Point>
typealias LineString = List<Point>
typealias Polygon = List<LinearRing>

class CheapRuler private constructor(latitude: Double, units: Units) {
	private val ky: Double
	private val kx: Double

	/**
	 * Computes the square of the distance between two points.
	 *
	 * @param a The first point
	 * @param b The second point
	 * @return The square of the distance between the two points
	 */
	fun squareDistance(a: Point, b: Point): Double {
		val dx = longDiff(a.lat, b.lat) * kx
		val dy: Double = (a.lon - b.lon) * ky
		return dx * dx + dy * dy
	}

	/**
	 * Computes the distance between two points.
	 *
	 * @param a The first point
	 * @param b The second point
	 * @return The distance between the two points
	 */
	fun distance(a: Point, b: Point): Double {
		return sqrt(squareDistance(a, b))
	}

	/**
	 * Computes the bearing between two points in angles.
	 *
	 * @param a The first point
	 * @param b The second point
	 * @return The bearing between the two points
	 */
	fun bearing(a: Point, b: Point): Double {
		val dx = longDiff(b.lat, a.lat) * kx
		val dy: Double = (b.lon - a.lon) * ky
		return atan2(dx, dy) / RAD
	}

	/**
	 * Computes a new point given distance and bearing from the starting point.
	 *
	 * @param origin  The point from which to start
	 * @param dist    The distance from the origin point
	 * @param bearing The bearing from the origin point
	 * @return A new point as indicated
	 */
	fun destination(origin: Point, dist: Double, bearing: Double): Point {
		val a = bearing * RAD
		return offset(origin, sin(a) * dist, Math.cos(a) * dist)
	}

	/**
	 * Computes a new point given easting and northing offsets from the starting
	 * point.
	 *
	 * @param origin The point from which to start
	 * @param dx     The easting offset
	 * @param dy     The northing offset
	 * @return A new point as indicated
	 */
	fun offset(origin: Point, dx: Double, dy: Double): Point {
		return Point(origin.lat + dx / kx, origin.lon + dy / ky)
	}

	/**
	 * Computes the distance along a line.
	 *
	 * @param points The line (an array of points)
	 * @return The distance
	 */
	fun lineDistance(points: LineString): Double {
		data class LineSum(val sum: Double = 0.0, val previous: Point? = null)
		return points.fold(LineSum()) { lineSum, element ->
			if (lineSum.previous == null)
				LineSum(previous = element)
			else LineSum(lineSum.sum+distance(lineSum.previous,element), element)
		}.sum
	}

	/**
	 * Computes the area of a polygon.
	 *
	 * @param poly The polygon (an array of rings, each of which is an array of points)
	 * @return The area
	 */
	fun area(poly: Polygon): Double {
		var sum = 0.0
		poly.forEachIndexed{ i, ring ->
			val len = ring.size
			var j = 0
			var k = len - 1
			while (j < len) {
				sum += (longDiff(ring[j].lat, ring[k].lat)
						* (ring[j].lon + ring[k].lon) * if (i != 0) -1.0 else 1.0)
				k = j++
			}
		}
		return abs(sum) / 2.0 * kx * ky
	}

	/**
	 * Computes a point at a specified distance along a line.
	 *
	 * @param line The line (an array of points)
	 * @param dist The distance along the line
	 * @return The indicated point
	 */
	fun along(line: LineString, dist: Double): Point {
		var sum = 0.0
		if (line.isEmpty()) {
			return Point(0.0, 0.0)
		}
		if (dist <= 0) {
			return line[0]
		}
		for (i in 0 until line.size - 1) {
			val p0: Point = line[i]
			val p1: Point = line[i + 1]
			val d = distance(p0, p1)
			sum += d
			if (sum > dist) {
				return interpolate(p0, p1, (dist - (sum - d)) / d)
			}
		}
		return line.get(line.size - 1)
	}

	/**
	 * Computes the distance from a point p to the line segment between points a and b.
	 *
	 * @param p The point in question
	 * @param a One end of the line segment
	 * @param b The other end of the line segment
	 * @return The indicated distance
	 */
	fun pointToSegmentDistance(p: Point, a: Point, b: Point): Double {
		var x = a.lat
		var y = a.lon
		val dx = longDiff(b.lat, x) * kx
		val dy = (b.lon - y) * ky
		if (dx != 0.0 || dy != 0.0) {
			val t = ((longDiff(p.lat, x) * kx * dx + (p.lon - y) * ky * dy)
					/ (dx * dx + dy * dy))
			if (t > 1.0) {
				x = b.lat
				y = b.lon
			} else if (t > 0.0) {
				x += dx / kx * t
				y += dy / ky * t
			}
		}
		return distance(p, Point(x, y))
	}

	/**
	 * Computes a tuple of the form &lt;point, index, t&gt; where point is the
	 * closest point on the line from the given point, index is the start index of
	 * the segment with the closest point, and t is a parameter from 0 to 1 that
	 * indicates where the closest point is on that segment.
	 *
	 * @param line The line (an array of points)
	 * @param p    The point in question
	 * @return The indicated tuple
	 */
	fun pointOnLine(line: LineString, p: Point): PointIndexT {
		var minDist = Double.POSITIVE_INFINITY
		var minX = 0.0
		var minY = 0.0
		var minT = 0.0
		var minI = 0
		if (line.isEmpty()) {
			return PointIndexT(Point(0.0, 0.0), 0, 0.0)
		}
		for (i in 0 until line.size - 1) {
			var t = 0.0
			var x = line[i].lat
			var y = line[i].lon
			val dx = longDiff(line[i + 1].lat, x) * kx
			val dy = (line[i + 1].lon - y) * ky
			if (dx != 0.0 || dy != 0.0) {
				t = (longDiff(p.lat, x) * kx * dx
						+ (p.lon - y) * ky * dy) / (dx * dx + dy * dy)
				if (t > 1) {
					x = line[i + 1].lat
					y = line[i + 1].lon
				} else if (t > 0) {
					x += dx / kx * t
					y += dy / ky * t
				}
			}
			val sqDist = squareDistance(p, Point(x, y))
			if (sqDist < minDist) {
				minDist = sqDist
				minX = x
				minY = y
				minI = i
				minT = t
			}
		}
		return PointIndexT(
				Point(minX, minY),
				minI,
				Math.max(0.0, Math.min(1.0, minT)))
	}

	/**
	 * Computes a part of the given line between the start and the stop points (or
	 * their closest points on the line).
	 *
	 * @param start The start point
	 * @param stop  The stop point
	 * @param line  The line (an array of points)
	 * @return The indicated portion of the line
	 */
	fun lineSlice(start: Point, stop: Point, line: LineString): LineString {
		var p1 = pointOnLine(line, start)
		var p2 = pointOnLine(line, stop)
		if (p1.index > p2.index
				|| p1.index == p2.index && p1.t > p2.t) {
			val tmp = p1
			p1 = p2
			p2 = tmp
		}
		val slice: MutableList<Point> = mutableListOf()
		slice.add(p1.point)
		val l: Int = p1.index + 1
		val r: Int = p2.index
		if (line[l] !== slice[0] && l <= r) {
			slice.add(line[l])
		}
		for (i in l + 1..r) {
			slice.add(line[i])
		}
		if (line[r] !== p2.point) {
			slice.add(p2.point)
		}
		return slice.toList()
	}

	/**
	 * Computes the part of the given line between the start and the stop points as
	 * indicated by distances along the line.
	 *
	 * @param start The distance from the start of the line to the start point
	 * @param stop  The distance from the start of the line to the stop point
	 * @param line  The line (an array of points)
	 * @return The indicated portion of the line
	 */
	fun lineSliceAlong(start: Double, stop: Double, line: LineString): LineString {
		var sum = 0.0
		val slice: MutableList<Point> = mutableListOf()
		for (i in 1 until line.size) {
			val p0: Point = line[i - 1]
			val p1: Point = line[i]
			val d = distance(p0, p1)
			sum += d
			if (sum > start && slice.size == 0) {
				slice.add(interpolate(p0, p1, (start - (sum - d)) / d))
			}
			if (sum >= stop) {
				slice.add(interpolate(p0, p1, (stop - (sum - d)) / d))
				return slice.toList()
			}
			if (sum > start) {
				slice.add(p1)
			}
		}
		return slice.toList()
	}

	/**
	 * Computes a bounding box object [w, s, e, n] centered on the given point and
	 * buffered by the given distance.
	 *
	 * @param p      The center point
	 * @param buffer The buffer distance
	 * @return The indicated bounding box
	 */
	fun bufferPoint(p: Point, buffer: Double): Box {
		val v = buffer / ky
		val h = buffer / kx
		return Box(
				Point(p.lat - h, p.lon - v),
				Point(p.lat + h, p.lon + v))
	}

	/**
	 * Computes a bounding box object [w, s, e, n] centered on the given box and
	 * buffered by the given distance.
	 *
	 * @param box    The center box
	 * @param buffer The buffer distance
	 * @return The indicated bounding box
	 */
	fun bufferBBox(box: Box, buffer: Double): Box {
		val v = buffer / ky
		val h = buffer / kx
		return Box(
				Point(box.min.lat - h, box.min.lon - v),
				Point(box.max.lat + h, box.max.lon + v))
	}

	companion object {
		// Values that define the WGS84 ellipsoid model of the Earth:
		private const val RE = 6378.137 // equatorial radius
		private const val FE = 1.0 / 298.257223563 // flattening
		private const val E2 = FE * (2 - FE)
		private const val RAD = Math.PI / 180.0

		/**
		 * Creates a CheapRuler instance valid for geodesic computations near the given
		 * latitude.
		 *
		 * @param latitude The latitude of interest, expressed in decimal degrees
		 * @param units     The distance unit to use in computations
		 * @return A CheapRuler instance
		 */
		fun fromLatitude(latitude: Double, units: Units): CheapRuler {
			return CheapRuler(latitude, units)
		}

		/**
		 * Creates a CheapRuler instance valid for the given tile coordinates.
		 *
		 * @param y    Y parameter of the tile of interest
		 * @param z    Z parameter of the tile of interest
		 * @param units The distance unit to use in computations
		 * @return A CheapRuler instance
		 */
		fun fromTile(y: Int, z: Int, units: Units): CheapRuler {
			require(y >= 0) { "y must be non-negative" }
			require(!(z < 0 || z >= 32)) {
				String.format(
						"z is out of the range [0, 32)", z)
			}
			val n = Math.PI * (1.0 - 2.0 * (y + 0.5) / (1 shl z))
			val latitude = atan(sinh(n)) / RAD
			return CheapRuler(latitude, units)
		}

		/**
		 * Tests whether the given point is inside in the given bounding box.
		 *
		 * @param p    The given point
		 * @param bbox The bounding box
		 * @return True if the point is inside the box, false otherwise.
		 */
		fun insideBBox(p: Point, bbox: Box): Boolean {
			return p.lon >= bbox.min.lon && p.lon <= bbox.max.lon && longDiff(p.lat, bbox.min.lat) >= 0 && longDiff(p.lat, bbox.max.lat) <= 0
		}

		/**
		 * Computes the point along a line segment at a given distance from the first
		 * endpoint.
		 *
		 * @param a One end of the line segment
		 * @param b The other end of the line segment
		 * @param t The distance from a
		 * @return The indicated point
		 */
		fun interpolate(a: Point, b: Point, t: Double): Point {
			val dx = longDiff(b.lat, a.lat)
			val dy: Double = b.lon - a.lon
			return Point(a.lat + dx * t, a.lon + dy * t)
		}

		private fun longDiff(a: Double, b: Double): Double {
			return (a - b).IEEErem(360.0)
		}
	}

	init {
		// Curvature formulas from https://en.wikipedia.org/wiki/Earth_radius#Meridional
		val mul = RAD * RE * units.multiplier
		val coslat = cos(latitude * RAD)
		val w2 = 1 / (1 - E2 * (1 - coslat * coslat))
		val w = sqrt(w2)

		// multipliers for converting longitude and latitude degrees into distance
		kx = mul * w * coslat // based on normal radius of curvature
		ky = mul * w * w2 * (1 - E2) // based on meridonal radius of curvature
	}
}