package com.bergerkiller.bukkit.nolagg.saving;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.SyncFailedException;
import java.lang.ref.Reference;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.minecraft.server.RegionFile;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileCacheRef;
import com.bergerkiller.bukkit.common.reflection.classes.RegionFileRef;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class RegionFileFlusher {
	private static Task flushTask;

	public static void reload() {
		Task.stop(flushTask);
		if (NoLaggSaving.writeDataEnabled) {
			flushTask = new Task(NoLagg.plugin) {
				public void run() {
					// get all the required region files to flush
					final List<Entry<RegionFile, RandomAccessFile>> regions = new ArrayList<Entry<RegionFile, RandomAccessFile>>();
					for (Reference<RegionFile> ref : RegionFileCacheRef.FILES.values()) {
						RegionFile regionfile = ref.get();
						if (regionfile != null) {
							RandomAccessFile raf = RegionFileRef.stream.get(regionfile);
							if (raf != null) {
								regions.add(new SimpleEntry<RegionFile, RandomAccessFile>(regionfile, raf));
							}
						}
					}

					// create an async task to write stuff to file
					new AsyncTask("NoLagg saving data writer") {
						public void run() {
							for (Entry<RegionFile, RandomAccessFile> file : regions) {
								synchronized (file.getKey()) {
									try {
										FileDescriptor fd = file.getValue().getFD();
										if (fd.valid()) {
											fd.sync();
										}
									} catch (SyncFailedException e) {
										// Suppress
									} catch (IOException e) {
										NoLaggSaving.plugin.log(Level.SEVERE, "Failed to sync region data to file:");
										e.printStackTrace();
									}
								}
							}
						}
					}.start(false);
				}
			}.start(NoLaggSaving.writeDataInterval, NoLaggSaving.writeDataInterval);
		}
	}

	public static void deinit() {
		Task.stop(flushTask);
		flushTask = null;
	}
}