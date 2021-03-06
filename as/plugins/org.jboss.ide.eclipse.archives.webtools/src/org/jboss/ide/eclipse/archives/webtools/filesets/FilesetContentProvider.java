/*******************************************************************************
 * Copyright (c) 2007 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.ide.eclipse.archives.webtools.filesets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.wst.server.core.IServer;
import org.jboss.ide.eclipse.as.core.server.internal.ExtendedServerPropertiesAdapterFactory;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.tools.as.wst.server.ui.xpl.ServerToolTip;

public class FilesetContentProvider implements ITreeContentProvider {
	private static final String FILESET_KEY = "org.jboss.ide.eclipse.as.ui.views.server.providers.FilesetViewProvider.PropertyKey"; //$NON-NLS-1$

	public static class PathWrapper {
		private IPath path;
		private IPath folder;

		public PathWrapper(IPath path, IPath folder) {
			this.path = path;
			this.folder = folder;
		}

		public IPath getFolder() {
			return folder;
		}

		public IPath getPath() {
			return folder.append(path);
		}

		public String getLocalizedResourceName() {
			return path.toOSString();
		}
		
		public boolean equals(Object o) {
			return o == null ? false :
						!(o instanceof PathWrapper) ? false :
							((PathWrapper)o).folder.equals(folder) && ((PathWrapper)o).path.equals(path);
		}
		
		public int hashCode() {
			return path.hashCode() + folder.hashCode();
		}
	}

	public static class FolderWrapper extends PathWrapper {
		private HashMap<String, FolderWrapper> childrenFolders;
		private ArrayList<PathWrapper> children;

		public FolderWrapper(IPath path, IPath folder) {
			super(path, folder);
			children = new ArrayList<PathWrapper>();
			childrenFolders = new HashMap<String, FolderWrapper>();
		}

		public void addChild(IPath path) {
			if (path.segmentCount() == 1) {
				children.add(new PathWrapper(path, getFolder().append(
						getLocalizedResourceName())));
			} else {
				addPath(children, childrenFolders, path, getFolder().append(
						getLocalizedResourceName()));
			}
		}

		public Object[] getChildren() {
			return children.toArray(new Object[children.size()]);
		}

		private void addPath(ArrayList<PathWrapper> children,
				HashMap<String, FolderWrapper> folders, IPath path, IPath folder) {
			FolderWrapper fw = null;
			if (path.segmentCount() > 0) {
				String seg1 = path.segment(0);
				if( !folders.containsKey(seg1)) {
					fw = new FolderWrapper(path.removeLastSegments(
							path.segmentCount() - 1), folder);
					folders.put(seg1, fw);
					children.add(fw);
				} else {
					fw = folders.get(seg1);
				}
				fw.addChild(path.removeFirstSegments(1));
			}
		}
	}

	public class ServerWrapper {
		public IServer server;
		private Fileset[] children;

		public ServerWrapper(IServer server) {
			this.server = server;
		}

		public int hashCode() {
			return server.getId().hashCode();
		}

		public boolean equals(Object other) {
			return other instanceof ServerWrapper
					&& ((ServerWrapper) other).server.getId().equals(
							server.getId());
		}

		public void addFileset(Fileset fs) {
			Fileset[] filesetsNew = new Fileset[children.length + 1];
			System.arraycopy(children, 0, filesetsNew, 0, children.length);
			filesetsNew[filesetsNew.length - 1] = fs;
			children = filesetsNew;
			saveFilesets();
		}

		public void removeFileset(Fileset fs) {
			ArrayList<Fileset> asList = new ArrayList<Fileset>(Arrays
					.asList(children));
			asList.remove(fs);
			children = asList.toArray(new Fileset[asList.size()]);
			saveFilesets();
		}

		public Fileset[] getFilesets() {
			if (children == null)
				children = loadFilesets();
			return children;
		}

		private Fileset[] loadFilesets() {
			if( FilesetUtil.getFile(server).exists()) {
				return FilesetUtil.loadFilesets(server);
			} 
			return new Fileset[0];
		}
		
		public void saveFilesets() {
			FilesetUtil.saveFilesets(server, children);
		}
	}
	
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IServer) {
			// check support
			ServerExtendedProperties props = ExtendedServerPropertiesAdapterFactory.getServerExtendedProperties((IServer)parentElement);
			if( props == null || !props.allowConvenienceEnhancements())
				return new Object[]{};
			
			return new Object[] { new ServerWrapper((IServer) parentElement) };
		}
		if (parentElement instanceof ServerWrapper) {
			return ((ServerWrapper) parentElement).getFilesets();
		} else if (parentElement instanceof Fileset) {
			Fileset fs = (Fileset) parentElement;
			IPath[] paths = null;
			try {
				paths = findPaths(fs);
			} catch (BuildException be) {
				return new Object[] {};
			}

			HashMap<String, FolderWrapper> folders = new HashMap<String, FolderWrapper>();
			ArrayList<PathWrapper> wrappers = new ArrayList<PathWrapper>();
			for (int i = 0; i < paths.length; i++) {
				if (paths[i].segmentCount() == 1) {
					wrappers.add(new PathWrapper(paths[i], new Path(fs
							.getFolder())));
				} else {
					addPath(wrappers, folders, paths[i], new Path(fs
							.getFolder()));
				}
			}
			return wrappers.toArray(new Object[wrappers.size()]);
		} else if (parentElement instanceof FolderWrapper) {
			return ((FolderWrapper) parentElement).getChildren();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0 ? true : false;
	}

	public Object[] getElements(Object inputElement) {
		return null;
	}

	public void dispose() {
	}

	private ServerToolTip tooltip = null;
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if( tooltip != null )
			tooltip.deactivate();
		tooltip = new ServerToolTip(((TreeViewer)viewer).getTree()) {
			
			@Override
			protected boolean isMyType(Object selected) {
				return selected instanceof ServerWrapper;
			}
			@Override
			protected void fillStyledText(Composite parent, StyledText sText, Object o) {
				sText.setText("Quickly access files matching a given pattern in your server installation."); //$NON-NLS-1$
			}
		};
		tooltip.setShift(new Point(15, 8));
		tooltip.setPopupDelay(500); // in ms
		tooltip.setHideOnMouseDown(true);
		tooltip.activate();
	}

	private IPath[] findPaths(Fileset fs) {
		return fs.findPaths();
	}

	private static void addPath(ArrayList<PathWrapper> children,
			HashMap<String, FolderWrapper> folders, IPath path, IPath folder) {
		try {
			FolderWrapper fw = null;
			if (!folders.containsKey(path.segment(0))) {
				fw = new FolderWrapper(path.removeLastSegments(path
						.segmentCount() - 1), folder);
				folders.put(path.segment(0), fw);
				children.add(fw);
			} else {
				fw = folders.get(path.segment(0));
			}
			fw.addChild(path.removeFirstSegments(1));
		} catch (Exception e) {
			// ignore
		}
	}
}