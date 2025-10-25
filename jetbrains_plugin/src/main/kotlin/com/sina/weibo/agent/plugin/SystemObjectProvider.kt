// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.plugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

/**
 * System Object Provider
 * Provides unified access to IDEA system objects
 */
object SystemObjectProvider {
    private val logger = Logger.getInstance(SystemObjectProvider::class.java)

    // Mapping for storing system objects per project instance
    private val projectObjects = ConcurrentHashMap<Project, ConcurrentHashMap<String, Any>>()

    /**
     * System object keys
     */
    object Keys {
        const val APPLICATION = "application"
        // More system object keys can be added
    }

    /**
     * Initialize the system object provider for a project
     * @param project current project
     */
    fun initialize(project: Project) {
        logger.info("Initializing SystemObjectProvider with project: ${project.name}")

        val objects = projectObjects.computeIfAbsent(project) { ConcurrentHashMap() }
        objects[Keys.APPLICATION] = ApplicationManager.getApplication()
    }

    /**
     * Register a system object for a project
     * @param project current project
     * @param key object key
     * @param obj object instance
     */
    fun register(project: Project, key: String, obj: Any) {
        val objects = projectObjects.computeIfAbsent(project) { ConcurrentHashMap() }
        objects[key] = obj
        logger.debug("Registered system object for project ${project.name}: $key")
    }

    /**
     * Get a system object for a project
     * @param project current project
     * @param key object key
     * @return object instance or null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(project: Project, key: String): T? {
        return projectObjects[project]?.get(key) as? T
    }

    /**
     * Clean up resources for a project
     */
    fun dispose(project: Project) {
        logger.info("Disposing SystemObjectProvider for project: ${project.name}")
        projectObjects.remove(project)?.clear()
    }

    /**
     * Clean up resources for all projects
     */
    fun disposeAll() {
        logger.info("Disposing SystemObjectProvider for all projects")
        projectObjects.clear()
    }
}
 