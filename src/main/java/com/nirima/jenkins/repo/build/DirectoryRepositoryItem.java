/*
 * The MIT License
 *
 * Copyright (c) 2011, Nigel Magnay / NiRiMa
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.nirima.jenkins.repo.build;

import com.nirima.jenkins.RepositoryPlugin;
import com.nirima.jenkins.repo.AbstractRepositoryElement;
import com.nirima.jenkins.repo.RepositoryContent;
import com.nirima.jenkins.repo.RepositoryDirectory;
import com.nirima.jenkins.repo.RepositoryElement;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represent a directory
 */
public class DirectoryRepositoryItem extends AbstractRepositoryElement implements RepositoryDirectory {

    private static final Logger LOGGER = Logger.getLogger(RepositoryPlugin.class.getName());

    protected Map<String, RepositoryElement> items;

    protected String name;
    private MetadataRepositoryItem mdItem = null;
    public DirectoryRepositoryItem(RepositoryDirectory parent, String name) {
        super(parent);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected Map<String, RepositoryElement> getItems()
    {
        if( items == null )
        {
            items = new HashMap<String, RepositoryElement>();
        }

        return items;
    }

    public Collection<? extends RepositoryElement> getChildren() {
        return getItems().values();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void insert(RepositoryContent content, String path, boolean allowOverwrite)
    {
        if( path.contains("/") )
        {
            int idx = path.indexOf("/");
            String dir = path.substring(0, idx);
            String rest = path.substring(idx+1);

            RepositoryElement dirElement = getChild(dir); // Get directory element
            if( dirElement == null )
            {
                // It doesn't already exist so create it:
                dirElement = add(new DirectoryRepositoryItem(this, dir), allowOverwrite);
            }

            // Insert into that directory.
            ((DirectoryRepositoryItem)dirElement).insert(content, rest, allowOverwrite);
        }
        else
        {
            // Not a path but a file for this location
            add(content, allowOverwrite);
        }
    }

    protected RepositoryElement add(RepositoryElement dirElement, boolean allowOverwrite)
    {
        if ( getItems().containsKey(dirElement.getName()) )
        {
            //LOGGER.warning("Already have element named " + dirElement.getName() + " for path " + getPath());
            if( !allowOverwrite )
                return items.get(dirElement.getName());
        }
        getItems().put(dirElement.getName(), dirElement);
        dirElement.setParent(this);
        if (dirElement instanceof ArtifactRepositoryItem) {
            addToMetadata((ArtifactRepositoryItem)dirElement);
        }
        return dirElement;
    }

    public RepositoryElement getChild(String element)
    {
       if (LOGGER.isLoggable(Level.FINE)) {
           LOGGER.fine("Get "+element+ " in "+getItems().keySet());
       }
       return getItems().get(element);
    }
    private void addToMetadata(ArtifactRepositoryItem item) {
        if (mdItem == null) {
            mdItem = new MetadataRepositoryItem(this, item.getBuild());
            this.add(mdItem, true);
            MetadataMD5RepositoryItem md5 = new MetadataMD5RepositoryItem(mdItem, this);
            this.add(md5, true);
            LOGGER.fine("Added "+mdItem.getName()+" to "+this.getPath());
        }
        LOGGER.fine("Adding "+item.getName()+" to "+mdItem.getPath());
        mdItem.addArtifact(item);
    }
}
