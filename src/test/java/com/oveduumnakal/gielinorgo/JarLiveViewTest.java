/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests the game camera's view of a jar in flight. */
public class JarLiveViewTest
{
	private static final int ZOOM = 700;
	private static final int[] YAWS = {0, 2048, 4096, 7000, 8192, 12288, 15000};
	private static final int[] PITCHES = {1100, 2048, 3000};

	/** A camera a given distance from the origin, looking at it. */
	private static JarLiveView.Camera camera(int yaw, int pitch, double distance)
	{
		return new JarLiveView.Camera(position(yaw, pitch, distance), JarLiveView.radians(yaw),
			JarLiveView.radians(pitch), ZOOM, 400, 300);
	}

	private static double[] position(int yaw, int pitch, double distance)
	{
		double turn = yaw * 2 * Math.PI / JarLiveView.CAMERA_TURN;
		double down = pitch * 2 * Math.PI / JarLiveView.CAMERA_TURN;
		double flat = distance * Math.cos(down);
		return new double[] {flat * Math.sin(turn), -flat * Math.cos(turn), -distance * Math.sin(down)};
	}

	@Test
	public void theCameraProjectsAsTheGameDoes()
	{
		for (int yaw : YAWS)
		{
			for (int pitch : PITCHES)
			{
				matchesTheGame(yaw, pitch, false);
				matchesTheGame(yaw, pitch, true);
			}
		}
	}

	/**
	 * Compares a still, upright jar's points with {@code Perspective.modelToCanvas}, which reads one camera with the
	 * GPU plugin on and another without it.
	 */
	private static void matchesTheGame(int yaw, int pitch, boolean gpu)
	{
		double[] eye = position(yaw, pitch, 900);
		int[] from = {(int) eye[0], (int) eye[1], (int) eye[2]};
		Client client = mock(Client.class);
		WorldView world = mock(WorldView.class);
		when(world.isTopLevel()).thenReturn(true);
		when(client.isGpu()).thenReturn(gpu);
		when(client.getCameraFpX()).thenReturn((float) from[0]);
		when(client.getCameraFpY()).thenReturn((float) from[1]);
		when(client.getCameraFpZ()).thenReturn((float) from[2]);
		when(client.getCameraFpYaw()).thenReturn((float) JarLiveView.radians(yaw));
		when(client.getCameraFpPitch()).thenReturn((float) JarLiveView.radians(pitch));
		when(client.getCameraX()).thenReturn(from[0]);
		when(client.getCameraY()).thenReturn(from[1]);
		when(client.getCameraZ()).thenReturn(from[2]);
		when(client.getCameraYaw()).thenReturn(yaw);
		when(client.getCameraPitch()).thenReturn(pitch);
		when(client.getScale()).thenReturn(ZOOM);
		when(client.getViewportWidth()).thenReturn(800);
		when(client.getViewportHeight()).thenReturn(600);
		JarLiveView.Camera camera = new JarLiveView.Camera(new double[] {from[0], from[1], from[2]},
			JarLiveView.radians(yaw), JarLiveView.radians(pitch), ZOOM, 400, 300);
		JarLiveView view = new JarLiveView(camera, new double[] {60, -40, -30}, 0, 0, 1);
		double unit = JarLiveView.JAR_HEIGHT / JarShapes.HEIGHT;
		double[][] points = {{40, 30, 20}, {-40, -50, 0}, {0, 60, -35}};
		for (double[] point : points)
		{
			float[] east = {(float) (point[0] * unit)};
			float[] north = {(float) (-point[2] * unit)};
			float[] down = {(float) (-point[1] * unit)};
			int[] x = new int[1];
			int[] y = new int[1];
			Perspective.modelToCanvas(client, world, 1, 60, -40, -30, 0, east, north, down, x, y);
			double[] mine = view.screen(view.view(point));
			assertEquals("x at yaw " + yaw + " pitch " + pitch, x[0], mine[0], 1.5);
			assertEquals("y at yaw " + yaw + " pitch " + pitch, y[0], mine[1], 1.5);
		}
	}

	@Test
	public void theJarTumblesEndOverEndTowardWhereItIsGoing()
	{
		double[][] headings = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}};
		JarLiveView.Camera fromAbove = new JarLiveView.Camera(new double[] {0, 0, -2000}, 0, Math.PI / 2, ZOOM, 400,
			300);
		for (double[] heading : headings)
		{
			JarLiveView view = new JarLiveView(fromAbove, new double[] {0, 0, 0}, 0.4, heading[0], heading[1]);
			double[] top = view.view(new double[] {0, 60, 0});
			double length = Math.hypot(heading[0], heading[1]);
			double forward = (top[0] * heading[0] + top[1] * heading[1]) / length;
			double aside = (top[0] * heading[1] - top[1] * heading[0]) / length;
			assertTrue("the top leads, heading " + heading[0] + "," + heading[1], forward > 5);
			assertEquals("and does not roll sideways", 0, aside, 0.001);
		}
	}

	@Test
	public void aJarLooksTheSameFromEverySideWhenItTurnsWithTheCamera()
	{
		JarModel jar = JarShapes.build(JarType.EMBER, true);
		BufferedImage north = picture(jar, 0);
		for (int quarter = 1; quarter < 4; quarter++)
		{
			BufferedImage turned = picture(jar, quarter * JarLiveView.CAMERA_TURN / 4);
			assertEquals(north.getWidth(), turned.getWidth(), 1);
			assertEquals(north.getHeight(), turned.getHeight(), 1);
		}
	}

	@Test
	public void everyFaceTurnedToTheCameraComesFromTheNearSide()
	{
		JarModel jar = JarShapes.build(JarType.IRON, true);
		for (int yaw : YAWS)
		{
			JarLiveView view = new JarLiveView(camera(yaw, 1100, 900), new double[] {0, 0, 0}, 0, 0, 1);
			for (JarModel.Face face : jar.faces())
			{
				if (face.layer() != JarModel.Layer.GLASS || face.cap() != 0)
					continue;

				double[] middle = new double[3];
				for (double[] point : face.points())
				{
					for (int k = 0; k < 3; k++)
						middle[k] += view.view(point)[k] / face.points().length;
				}

				double[] centre = view.view(new double[] {0, middle[1], 0});
				double[] out = {middle[0] - centre[0], 0, middle[2] - centre[2]};
				if (middle[2] > 15)
					assertTrue("yaw " + yaw, view.faces(middle, out));
			}
		}
	}

	@Test
	public void theHighlightsStayOnTheSideFacingTheCamera()
	{
		for (int yaw : YAWS)
		{
			for (double turns = 0; turns < 2 * Math.PI; turns += 0.5)
			{
				JarLiveView view = new JarLiveView(camera(yaw, 1100, 900), new double[] {0, 0, 0}, turns, 0, 1);
				double[] front = view.reflection(new double[] {0, 0, 40});
				assertTrue("yaw " + yaw + " turns " + turns, front[2] > 0);
				assertTrue(view.reflectionStrength() >= 0 && view.reflectionStrength() <= 1);
			}
		}
	}

	@Test
	public void aJarGrowsAsTheCameraComesCloser()
	{
		JarModel jar = JarShapes.build(JarType.RUNE, true);
		Rectangle far = JarRenderer.bounds(jar, new JarLiveView(camera(0, 1100, 1500), new double[3], 0, 0, 1));
		Rectangle near = JarRenderer.bounds(jar, new JarLiveView(camera(0, 1100, 500), new double[3], 0, 0, 1));
		assertTrue(near.height > 2 * far.height);
	}

	@Test
	public void aJarBehindTheCameraIsNotDrawn()
	{
		JarLiveView.Camera camera = new JarLiveView.Camera(new double[] {0, 0, 0}, 0, 0, ZOOM, 400, 300);
		assertTrue(new JarLiveView(camera, new double[] {0, 500, 0}, 0, 0, 1).inFront());
		assertFalse(new JarLiveView(camera, new double[] {0, -500, 0}, 0, 0, 1).inFront());
	}

	private static BufferedImage picture(JarModel jar, int yaw)
	{
		double turn = -yaw * 2 * Math.PI / JarLiveView.CAMERA_TURN;
		JarLiveView.Camera camera = camera(yaw, 1100, 600);
		JarLiveView view = new JarLiveView(camera, new double[] {0, 0, 0}, 0, Math.sin(turn), Math.cos(turn));
		return JarRenderer.render(jar, view, JarRenderer.bounds(jar, view), 2, 0.5, 0xB4);
	}
}
