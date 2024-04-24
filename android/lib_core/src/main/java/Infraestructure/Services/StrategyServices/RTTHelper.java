package Infraestructure.Services.StrategyServices;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

import Models.Node;

public class RTTHelper {

    private int nRow = 0;

    public Node getMidpointOfFirstWhiteLine(Bitmap image) throws Exception {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int midpointX = -1;
        int midpointY = -1;
        boolean foundWhiteLine = false;

        for (int y = imageHeight - 1; y >= 0; y--) {
            for (int x = imageWidth/2; x < imageWidth; x++) {
                int pixel = image.getPixel(x, y);
                if (isWhitePixel(pixel)) {
                    double whitePercentageLeft = calculateWhitePercentage(image, x - 1, y);
                    double whitePercentageRight = calculateWhitePercentage(image, x + 1, y);

                    if (whitePercentageLeft >= 0.3 && whitePercentageRight >= 0.3) {
                        midpointX = x;
                        midpointY = y;
                        foundWhiteLine = true;
                        break;
                    }
                }
            }
            if (foundWhiteLine) {
                break;
            }
        }

        if (midpointX != -1 && midpointY != -1) {
            Node midpoint = new Node(midpointX, midpointY);
            return midpoint;
        } else {
            throw new Exception("Nenhuma linha branca encontrada na imagem com vizinhança adequada.");
        }
    }

    private boolean isWhitePixel(int pixel) {
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int whiteThreshold = 100;

        return (red >= whiteThreshold) && (green >= whiteThreshold) && (blue >= whiteThreshold);
    }
    public Node getMidpointOfFirstWhiteLineTop(Bitmap image) throws Exception {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int midpointX = -1;
        int midpointY = -1;
        boolean foundWhiteLine = false;

        for (int y = 0; y < imageHeight/2; y++) {
            for (int x = imageWidth/2; x < imageWidth; x++) {
                int pixel = image.getPixel(x, y);
                if (isWhitePixel(pixel)) {
                    double whitePercentageLeft = calculateWhitePercentage(image, x - 1, y);
                    double whitePercentageRight = calculateWhitePercentage(image, x + 1, y);

                    if (whitePercentageLeft >= 0.4 && whitePercentageRight >= 0.4) {
                        boolean hasMagentaColor = hasMagentaColorInRadius(image, x, y, 10);
                        if (!hasMagentaColor) {
                            midpointX = x;
                            midpointY = y;
                            foundWhiteLine = true;
                            break;
                        }
                    }
                }
            }
            if (foundWhiteLine) {
                break;
            }
        }

        if (midpointX != -1 && midpointY != -1) {
            Node midpoint = new Node(midpointX, midpointY);
            return midpoint;
        } else {
            throw new Exception("Nenhuma linha branca encontrada no topo da imagem com vizinhança adequada.");
        }
    }

    private boolean hasMagentaColorInRadius(Bitmap image, int x, int y, int radius) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int nx = x + i;
                int ny = y + j;

                if (nx >= 0 && nx < imageWidth && ny >= 0 && ny < imageHeight) {
                    int pixel = image.getPixel(nx, ny);
                    if (pixel == Color.MAGENTA) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private double calculateWhitePercentage(Bitmap image, int x, int y) {
        int whiteCount = 0;
        int totalCount = 0;
        int radius = 1;

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                int nx = x + i;
                int ny = y + j;

                if (nx >= 0 && nx < imageWidth && ny >= 0 && ny < imageHeight) {
                    int pixel = image.getPixel(nx, ny);
                    if (isWhitePixel(pixel)) {
                        whiteCount++;
                    }
                    totalCount++;
                }
            }
        }

        return (double) whiteCount / totalCount;
    }

    public List<PointF> convertNodesToPositions(List<Node> nodeList, float imageWidth, float sceneWidthCm)
    {
        List<PointF> positionList = new ArrayList<>();

        float imageCenter = imageWidth / 2.0f;
        float sceneCenter = sceneWidthCm / 2.0f;

        float firstNodeY = (float) nodeList.get(0).getY();

        for (Node node : nodeList) {
            float adjustedY = firstNodeY - (float) node.getY();

            float scaledX = ((float) node.getX() - imageCenter) / imageCenter * sceneCenter;
            float scaledY = adjustedY / imageCenter * sceneCenter;

            PointF position = new PointF(scaledX, scaledY);
            positionList.add(position);
        }

        return positionList;
    }

    public void paintRectangle(Bitmap image, int x, int y, int width, int height, int color) {
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                if (i >= 0 && i < image.getWidth() && j >= 0 && j < image.getHeight()) {
                    image.setPixel(i, j, color);
                }
            }
        }
    }

    public List<Node> getEquidistantNodes(List<Node> nodes, int numPoints) {
        List<Node> equidistantNodes = new ArrayList<>();

        if (nodes.isEmpty() || numPoints < 2) {
            return equidistantNodes;
        }

        double totalDistance = calculateTotalDistance(nodes);
        double spacing = totalDistance / (numPoints - 1);

        equidistantNodes.add(nodes.get(0));

        double currentDistance = 0.0;
        double accumulatedDistance = 0.0;

        for (int i = 1; i < nodes.size(); i++) {
            Node currentNode = nodes.get(i);
            Node previousNode = nodes.get(i - 1);

            double segmentDistance = calculateDistance(previousNode, currentNode);

            if (currentDistance + segmentDistance >= spacing) {
                double remainingDistance = spacing - currentDistance;
                double ratio = remainingDistance / segmentDistance;
                double newX = previousNode.getX() + ratio * (currentNode.getX() - previousNode.getX());
                double newY = previousNode.getY() + ratio * (currentNode.getY() - previousNode.getY());
                Node equidistantNode = new Node(newX, newY);
                equidistantNodes.add(equidistantNode);
                accumulatedDistance += spacing;
                currentDistance = segmentDistance - remainingDistance;

                if (accumulatedDistance >= totalDistance) {
                    break;
                }
            } else {
                currentDistance += segmentDistance;
            }
        }

        equidistantNodes.add(nodes.get(nodes.size() - 1));

        return equidistantNodes;
    }

    public double calculateDistance(Node node1, Node node2) {
        double dx = node2.getX() - node1.getX();
        double dy = node2.getY() - node1.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double calculateTotalDistance(List<Node> path) {
        double totalDistance = 0.0;
        int pathSize = path.size();

        for (int i = 0; i < pathSize - 1; i++) {
            Node currentNode = path.get(i);
            Node nextNode = path.get(i + 1);
            double distance = calculateDistance(currentNode, nextNode);
            totalDistance += distance;
        }

        return totalDistance;
    }
}
