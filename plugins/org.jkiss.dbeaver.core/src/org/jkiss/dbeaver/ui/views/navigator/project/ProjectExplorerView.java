/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.views.navigator.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.resources.DefaultResourceHandlerImpl;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ViewerColumnController;
import org.jkiss.dbeaver.ui.views.navigator.database.NavigatorViewBase;
import org.jkiss.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * ProjectExplorerView
 */
public class ProjectExplorerView extends NavigatorViewBase implements DBPProjectListener
{

    //static final Log log = LogFactory.getLog(ProjectExplorerView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectExplorer";

    public ProjectExplorerView() {
        DBeaverCore.getInstance().getProjectRegistry().addProjectListener(this);
    }

    @Override
    public DBNNode getRootNode()
    {
        DBNProject projectNode = getModel().getRoot().getProject(DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
        return projectNode != null ? projectNode : getModel().getRoot();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        final TreeViewer viewer = getNavigatorViewer();
        viewer.getTree().setHeaderVisible(true);
        createColumns(viewer);
        UIUtils.setHelp(parent, IHelpContextIds.CTX_PROJECT_EXPLORER);

        this.getNavigatorViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return !(element instanceof DBNProjectDatabases);
            }
        });
        updateTitle();
    }

    private void createColumns(TreeViewer viewer)
    {
        final LabelProvider mainLabelProvider = (LabelProvider)viewer.getLabelProvider();
        ViewerColumnController columnController = new ViewerColumnController("projectExplorer", viewer);
        columnController.addColumn("Name", "Resource name", SWT.LEFT, true, true, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                cell.setImage(mainLabelProvider.getImage(cell.getElement()));
                cell.setText(mainLabelProvider.getText(cell.getElement()));
            }
        });

        columnController.addColumn("DataSource", "Datasource(s) associated with resource", SWT.LEFT, true, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DBNNode node = (DBNNode) cell.getElement();
                if (node instanceof DBNDatabaseNode) {
                    cell.setText(((DBNDatabaseNode) node).getDataSourceContainer().getName());
                } else if (node instanceof DBNResource) {
                    Collection<DBSDataSourceContainer> containers = ((DBNResource) node).getAssociatedDataSources();
                    if (!CommonUtils.isEmpty(containers)) {
                        StringBuilder text = new StringBuilder();
                        for (DBSDataSourceContainer container : containers) {
                            if (text.length() > 0) {
                                text.append(", ");
                            }
                            text.append(container.getName());
                        }
                        cell.setText(text.toString());
                    }
                } else {
                    cell.setText("");
                }
            }
        });
        columnController.addColumn("Size", "File size", SWT.LEFT, false, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DBNNode node = (DBNNode) cell.getElement();
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    if (resource instanceof IFile) {
                        cell.setText(String.valueOf(resource.getLocation().toFile().length()));
                    }
                }
            }
        });
        columnController.addColumn("Modified", "Time the file was last modified", SWT.LEFT, false, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DBNNode node = (DBNNode) cell.getElement();
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    if (resource instanceof IFile) {
                        cell.setText(
                            SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT)
                                .format(new Date(resource.getLocation().toFile().lastModified())));
                    }
                }
            }
        });
        columnController.addColumn("Type", "Resource type", SWT.LEFT, false, false, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DBNNode node = (DBNNode) cell.getElement();
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    DefaultResourceHandlerImpl.ProgramInfo program = DefaultResourceHandlerImpl.getProgram(resource);
                    if (program != null) {
                        cell.setText(program.getProgram().getName());
                    }
                }
            }
        });
        columnController.createColumns();
    }

    @Override
    protected int getTreeStyle()
    {
        return super.getTreeStyle() | SWT.FULL_SELECTION;
    }

    @Override
    public void dispose()
    {
        DBeaverCore.getInstance().getProjectRegistry().removeProjectListener(this);
        super.dispose();
    }

    @Override
    public void handleActiveProjectChange(IProject oldValue, IProject newValue)
    {
        getNavigatorTree().reloadTree(getRootNode());
        //UIUtils.packColumns(getNavigatorTree().getViewer().getTree());
        updateTitle();
    }

    private void updateTitle()
    {
        setPartName("Project - " + getRootNode().getNodeName());
    }

}
