import Anthropic from "@anthropic-ai/sdk"
import { ChatCompletionCreateParamsNonStreaming, ChatCompletionCreateParamsStreaming } from "openai/resources/index.mjs"
import { getTaskMsg } from "../../utils/task"

export function weCoderGenerateOpenAiMessageConfig(
	taskId: any,
	gitUrl: string,
	messages: Anthropic.Messages.MessageParam[],
	requestOptions: ChatCompletionCreateParamsStreaming,
): ChatCompletionCreateParamsStreaming {
	const [exist, taskMessage] = getTaskMsg(taskId, messages)

	return {
		...requestOptions,
		wecode_message: exist ? "" : taskMessage,
		git_url: gitUrl,
	} as ChatCompletionCreateParamsStreaming
}

export function weCoderGenerateOpenAiNoStreamMessageConfig(
	taskId: any,
	gitUrl: string,
	messages: Anthropic.Messages.MessageParam[],
	requestOptions: ChatCompletionCreateParamsNonStreaming,
): ChatCompletionCreateParamsNonStreaming {
	const [exist, taskMessage] = getTaskMsg(taskId, messages)

	return {
		...requestOptions,
		wecode_message: exist ? "" : taskMessage,
		git_url: gitUrl,
	} as ChatCompletionCreateParamsNonStreaming
}
