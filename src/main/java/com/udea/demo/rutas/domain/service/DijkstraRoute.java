package com.udea.demo.rutas.domain.service;

import java.util.*;

public final class DijkstraRoute {
    public record Stop(Long id, double latitude, double longitude) {}
    public record Edge(Long from, Long to, double distance) {}

    private DijkstraRoute() {}

    public static List<Long> order(List<Stop> stops) {
        if (stops == null || stops.isEmpty()) return List.of();
        List<Stop> located = stops.stream().filter(s -> Double.isFinite(s.latitude()) && Double.isFinite(s.longitude())).toList();
        List<Long> result = new ArrayList<>();
        Set<Long> remaining = new LinkedHashSet<>(located.stream().map(Stop::id).toList());
        Stop current = located.get(0);
        result.add(current.id());
        remaining.remove(current.id());
        Map<Long, List<Edge>> graph = new HashMap<>();
        for (Stop from : located) {
            graph.put(from.id(), located.stream().filter(to -> !from.id().equals(to.id()))
                    .map(to -> new Edge(from.id(), to.id(), haversine(from, to))).toList());
        }
        while (!remaining.isEmpty()) {
            Stop next = null;
            double best = Double.POSITIVE_INFINITY;
            for (Stop candidate : located) {
                if (remaining.contains(candidate.id())) {
                    List<Long> path = shortestPath(current.id(), candidate.id(), graph);
                    double distance = pathDistance(path, graph);
                    if (distance < best) {
                        best = distance;
                        next = candidate;
                    }
                }
            }
            if (next == null) break;
            result.add(next.id());
            remaining.remove(next.id());
            current = next;
        }
        return List.copyOf(result);
    }

    private static double pathDistance(List<Long> path, Map<Long, List<Edge>> graph) {
        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            Long from = path.get(i - 1);
            Long to = path.get(i);
            total += graph.getOrDefault(from, List.of()).stream()
                    .filter(edge -> edge.to().equals(to)).mapToDouble(Edge::distance).findFirst()
                    .orElse(Double.POSITIVE_INFINITY);
        }
        return total;
    }

    public static double haversine(Stop a, Stop b) {
        double lat = Math.toRadians(b.latitude() - a.latitude());
        double lon = Math.toRadians(b.longitude() - a.longitude());
        double x = Math.sin(lat / 2) * Math.sin(lat / 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude()))
                * Math.sin(lon / 2) * Math.sin(lon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x));
    }

    public static List<Long> shortestPath(Long origin, Long destination, Map<Long, List<Edge>> graph) {
        if (origin == null || destination == null || graph == null || !graph.containsKey(origin)
                || !graph.containsKey(destination)) return List.of();
        Map<Long, Double> distance = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<Long> queue = new PriorityQueue<>(Comparator.comparingDouble(n -> distance.getOrDefault(n, Double.POSITIVE_INFINITY)));
        graph.keySet().forEach(n -> distance.put(n, Double.POSITIVE_INFINITY));
        distance.put(origin, 0.0);
        queue.add(origin);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current.equals(destination)) break;
            for (Edge edge : graph.getOrDefault(current, List.of())) {
                double candidate = distance.get(current) + edge.distance();
                if (candidate < distance.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    distance.put(edge.to(), candidate);
                    previous.put(edge.to(), current);
                    queue.add(edge.to());
                }
            }
        }
        if (distance.get(destination).isInfinite()) return List.of();
        LinkedList<Long> path = new LinkedList<>();
        for (Long node = destination; node != null; node = previous.get(node)) path.addFirst(node);
        return List.copyOf(path);
    }
}
