import { MessageCreateParamsStreaming } from "@anthropic-ai/sdk/resources"
import { getTaskMsg } from "../../utils/task"
import Anthropic from "@anthropic-ai/sdk"

export function weCoderGenerateAnthropicMessageConfig(
	taskId: any,
	gitUrl: string,
	messages: Anthropic.Messages.MessageParam[],
	body: MessageCreateParamsStreaming,
): MessageCreateParamsStreaming {
	const [exist, taskMessage] = getTaskMsg(taskId, messages)
	return {
		...body,
		wecode_message: exist ? "" : taskMessage,
		git_url: gitUrl,
	} as MessageCreateParamsStreaming
}
