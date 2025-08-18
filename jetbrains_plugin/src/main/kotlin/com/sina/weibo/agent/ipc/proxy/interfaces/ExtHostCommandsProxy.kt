// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.ipc.proxy.interfaces

import com.sina.weibo.agent.ipc.proxy.LazyPromise

//export interface ExtHostCommandsShape {
//    $executeContributedCommand(id: string, ...args: any[]): Promise<unknown>;
//    $getContributedCommandMetadata(): Promise<{ [id: string]: string | ICommandMetadataDto }>;
//}

interface ExtHostCommandsProxy {
    fun executeContributedCommand(id: String, args: List<Any?>) : LazyPromise
    fun executeContributedCommand(id: String, vararg args: Any?) : LazyPromise
    fun executeContributedCommand(id: String): LazyPromise
    fun getContributedCommandMetadata() : LazyPromise
}