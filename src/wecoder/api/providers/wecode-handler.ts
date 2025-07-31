import Anthropic from "@anthropic-ai/sdk"
import { getCache, ModelMeta, setCache } from "../../utils/cache/taskCache"
import { getTaskMsg } from "../../utils/task"

export function wecodeHandlerChunk(
	taskId: string | undefined,
	modelResponseId: string | undefined,
	modelId: string,
	messages: Anthropic.Messages.MessageParam[],
	mode?: string,
): void {
	if (!taskId) {
		return
	}

	const cache = getCache(taskId)
	if (cache && cache.id === modelResponseId) {
		return
	}

	const m: ModelMeta = {
		id: modelResponseId || crypto.randomUUID(),
		modelId: modelId,
		taskMessage: getTaskMsg(taskId, messages)[1],
		mode: mode,
	}
	setCache(taskId, m)
}
