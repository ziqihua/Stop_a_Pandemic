import org.junit.Before;
import org.junit.Test;


import java.util.*;

import static org.junit.Assert.*;

public class InformationSpreadTest {
    private InformationSpread informationSpread;

    @Before
    public void setUp() {
        this.informationSpread = new InformationSpread();
    }

    @Test
    public void testLoadGraphFromDataSet() {
        String testFile = "test_graph.mtx";
        double tau = 0.55;
        int correctNodeCount = 8;
        int outputNodeCount = informationSpread.loadGraphFromDataSet(testFile, tau);
        assertEquals(correctNodeCount, outputNodeCount);
    }

    @Test
    public void testGetNeighborsWithConnections() {
        String testFile = "test_graph.mtx";
        double tau = 0.55;
        informationSpread.loadGraphFromDataSet(testFile, tau);

        int id = 1;
        int[] expectedNeighbors = {2, 3};
        int[] actualNeighbors = informationSpread.getNeighbors(id);
        assertArrayEquals(expectedNeighbors, actualNeighbors);
        int nodeId2 = 10;
        int[] expectedNeighbors2 = {9,12};
        int[] actualNeighbors2 = informationSpread.getNeighbors(nodeId2);
        assertArrayEquals(expectedNeighbors2, actualNeighbors2);
    }

    @Test
    public void testGetNeighborsNoConnections() {
        String testFile = "test_graph.mtx";
        double tau = 0.55;
        informationSpread.loadGraphFromDataSet(testFile, tau);

        int id = 6;
        int[] expectedNeighbors = new int[0];

        int[] actualNeighbors = informationSpread.getNeighbors(id);
        assertArrayEquals(expectedNeighbors, actualNeighbors);
    }

    @Test
    public void testPath() {
        String testFile = "test_graph.mtx";
        double tau = 0.55;
        informationSpread.loadGraphFromDataSet(testFile, tau);

        int source = 1;
        int destination = 3;
        List<Integer> expectedPath = Arrays.asList(1, 3);

        List<Integer> actualPath = informationSpread.path(source, destination);
        assertEquals(expectedPath, actualPath);

        int source2 = 6;
        int destination2 = 9;

        List<Integer> actualPath2 = informationSpread.path(source2, destination2);
        assertTrue(actualPath2.isEmpty());
    }

    @Test
    public void testPathWithSameSourceAndDest() {
        String testFile = "test_graph.mtx";
        double tau = 0.55;
        informationSpread.loadGraphFromDataSet(testFile, tau);
        int node = 3;
        List<Integer> expectedPath = Collections.singletonList(node);
        List<Integer> actualPath = informationSpread.path(node, node);
        assertEquals(expectedPath, actualPath);
    }

    @Test
    public void testAvgDegree() {
        String testFile = "test_graph.mtx";
        double tau = 0.55;
        informationSpread.loadGraphFromDataSet(testFile, tau);
        double expectedDegree = 14.0 / 12.0;
        double actualDegree = informationSpread.avgDegree();
        assertEquals(expectedDegree, actualDegree, 0.001);
    }

    @Test
    public void testAvgDegreeWithOneNode() {
        informationSpread.loadGraphFromDataSet("one_node_graph.mtx", 0.55);
        double expectedAvgDegree = 2.0;
        double actualAvgDegree = informationSpread.avgDegree();
        assertEquals(expectedAvgDegree, actualAvgDegree, 0.001);
    }

    @Test
    public void testRNumberZeroDegree() {
        informationSpread.loadGraphFromDataSet("nothing_graph.mtx", 0.55);
        double expectedR0 = 0.0;
        double actualR0 = informationSpread.rNumber();
        assertEquals(expectedR0, actualR0, 0.001);
    }

    @Test
    public void testRNumber() {
        String testFile = "test_graph.mtx";
        double tau = 0.55;
        informationSpread.loadGraphFromDataSet(testFile, tau);
        double expectedAvgDegree = 14.0 / 12.0;
        double expectedR = tau * expectedAvgDegree;
        double actualR = informationSpread.rNumber();
        assertEquals(expectedR, actualR, 0.001);
    }

    @Test
    public void testGenerations() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        int seed = 1;
        double threshold = 0.3;
        int expectedGenerations = 2;
        int actualGenerations = informationSpread.generations(seed, threshold);
        assertEquals(expectedGenerations, actualGenerations);

        threshold = 1.0;
        actualGenerations = informationSpread.generations(seed, threshold);
        expectedGenerations = -1;
        assertEquals(expectedGenerations, actualGenerations);
    }

    @Test
    public void testDegree() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        assertEquals(2, informationSpread.degree(1));
        assertEquals(2, informationSpread.degree(7));
        assertEquals(-1, informationSpread.degree(200));
    }

    @Test
    public void testNodesWithSpecificDegree() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        Collection<Integer> expectedNodesWithDegree2 =
                new HashSet<>(Arrays.asList(1, 2, 3, 7, 9, 10));
        Collection<Integer> actualNodesWithDegree2 = informationSpread.degreeNodes(2);
        assertEquals(expectedNodesWithDegree2, actualNodesWithDegree2);
        assertTrue(informationSpread.degreeNodes(100).isEmpty());
    }

    @Test
    public void testGenerationsAfterDegreeRemoved() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        int seed = 1;
        double threshold = 0.1;
        int degreeToRemove = 1;

        int actualGenerations =
                informationSpread.generationsDegree(seed, threshold, degreeToRemove);
        assertEquals(1, actualGenerations);

        int seed1 = 1;
        double threshold1 = 0.5;
        int degreeToRemove1 = 100;
        int actualGenerations1 =
                informationSpread.generationsDegree(seed1, threshold1, degreeToRemove1);
        assertEquals(-1, actualGenerations1);
    }

    @Test
    public void testRNumberAfterDegreeRemoved() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        double expectedRNum = 0.55 * 10 / 12;
        double actualRNum = informationSpread.rNumberDegree(1);
        assertEquals(expectedRNum, actualRNum, 0.01);

        double actualRNum1 = informationSpread.rNumberDegree(100);
        assertEquals(informationSpread.rNumber(), actualRNum1, 0.01);
    }

    @Test
    public void testClustCoeff() {
        String testFile = "tri_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.01);

        double actualClustCoeffNodeId1 = informationSpread.clustCoeff(1);
        assertEquals(1.0, actualClustCoeffNodeId1, 0.01);
    }

    @Test
    public void testClustCoeffNodes() {
        String testFile = "tri_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.01);
        Collection<Integer> expectedNodesCollection = new HashSet<>(Arrays.asList(1, 2, 3));
        assertEquals(expectedNodesCollection, informationSpread.clustCoeffNodes(0.5, 1.0));
    }

    @Test
    public void testGenerationsCC() {
        String testFile = "tri_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.01);
        int seed = 1;
        double low = 0.2;
        double high = 1.0;
        double threshold = 0.5;

        int actualGeneration = informationSpread.generationsCC(seed, threshold, low, high);
        assertEquals(0, actualGeneration);

        String testFile1 = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile1, 0.01);

        int actualGeneration1 =
                informationSpread.generationsCC(seed, threshold, low, high);
        assertEquals(-1, actualGeneration1);
    }

    @Test
    public void testGenerationsCCWrongSeed() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        int seed = -250;
        double low = 0.5;
        double high = 1.0;
        double threshold = 0.8;
        assertEquals(-1, informationSpread.generationsCC(seed, threshold, low, high));
    }

    @Test
    public void testRNumberCC() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        double low = 0.2;
        double high = 0.7;
        double expectedR = 0.55 * 14 / 12;
        assertEquals(expectedR, informationSpread.rNumberCC(low, high), 0.01);
    }

    @Test
    public void testHighDegLowCCNodes() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        int low = 2;
        double up = 0.5;

        Set<Integer> expectedNodesCollection = new HashSet<>(Arrays.asList(1, 2, 3, 7, 9, 10));

        assertEquals(expectedNodesCollection, informationSpread.highDegLowCCNodes(low, up));
    }

    @Test
    public void testHighDegLowCCNodesNone() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        int low = 250;
        double up = 2.5;
        assertTrue(informationSpread.highDegLowCCNodes(low, up).isEmpty());
    }

    @Test
    public void testGenerationsHighDegLowCC() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        int seed = 1;
        double threshold = 0.1;
        int low = 2;
        double up = 0.3;
        assertEquals(0, informationSpread.generationsHighDegLowCC(seed,
                threshold, low, up));
    }

    @Test
    public void testGenerationsHighDegLowCCWrongSeed() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        int seed = -100;
        double threshold = 0.1;
        int low = 2;
        double up = 0.3;
        assertEquals(-1, informationSpread.generationsHighDegLowCC(seed,
                threshold, low, up));
    }

    @Test
    public void testRNumberDegCC() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        assertEquals(0, informationSpread.rNumberDegCC(2, 0.3), 0.01);
    }

    @Test
    public void testRNumberDegCCNodesNone() {
        String testFile = "test_graph.mtx";
        informationSpread.loadGraphFromDataSet(testFile, 0.55);
        assertEquals(informationSpread.rNumber(),
                informationSpread.rNumberDegCC(250, 3.0), 0.01);
    }
}
