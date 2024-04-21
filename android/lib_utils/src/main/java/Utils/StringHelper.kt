package Utils

import android.graphics.PointF

class StringHelper
{
    fun convertPointsToString(points: List<PointF>): String {
        val stringBuilder = StringBuilder()

        for (point in points)
        {
            stringBuilder.append(point.x)
                    .append(",")
                    .append(point.y)
                    .append(";")
        }

        if (stringBuilder.isNotEmpty()) {
            stringBuilder.deleteCharAt(stringBuilder.length - 1)
        }
        return stringBuilder.toString()
    }
}