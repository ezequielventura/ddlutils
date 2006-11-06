package org.apache.ddlutils.task;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.ddlutils.model.Database;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Base interface for commands that work with a model.
 * 
 * @version $Revision: 289996 $
 * @ant.type ignore="true"
 */
public interface Command
{
    /**
     * Specifies whether this command requires a model, i.e. whether the second
     * argument in {@link #execute(Task, Database)} cannot be <code>null</code>.
     * 
     * @return <code>true</code> if this command requires a model 
     */
    public boolean isRequiringModel();

    /**
     * Executes this command.
     * 
     * @param task  The executing task
     * @param model The database model
     */
    public void execute(Task task, Database model) throws BuildException;
}
