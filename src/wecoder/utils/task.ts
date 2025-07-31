import Anthropic from "@anthropic-ai/sdk"
import { getCache } from "../utils/cache/taskCache"

export function getTaskMsg(taskId: any, messages: Anthropic.Messages.MessageParam[]): [boolean, string] {
	// 用户输入信息
	let message = ""
	for (const chunk of messages) {
		if (chunk.role === "user") {
			// eslint-disable-next-line no-unsafe-optional-chaining
			for (const part of chunk?.content) {
				if (typeof part === "string") {
					continue
				}

				if (part && part?.type === "text" && part?.text.includes("<task>")) {
					message = part?.text.replace("<task>\n", "").replace("\n</task>", "")
					// 任务信息中包含文件内容，需要去掉
					message = message.split("<file_content")[0]
					break
				}
			}
		}
	}

	if (message === "") {
		return [false, ""]
	}

	const cache = taskId ? getCache(taskId) : null
	if (cache?.taskMessage === message) {
		return [true, message]
	}
	return [false, message]
}
