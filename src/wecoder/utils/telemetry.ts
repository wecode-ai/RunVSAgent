import { generateHeaders } from "./wecode-headers"
import { UserActionEnum } from "./userAction"
import { WECODE_PROXY_URL } from "../../api/providers/wecoder_constants"

export interface recordParam {
	id?: string // 大模型唯一返回id
	modelId?: string // 大模型id
	taskId?: string // cline 的 taskId, 充当 chat-session-id
	action?: string
	language?: string
	lineAddCount?: number
	codeAddContents?: string[]
	deleteLineCount?: number
	codeDeleteContents?: string[]
	filePath?: string
	gitUrl?: string
	mode?: string // 当前使用的模式
}

export type TelemetryUserResponse = object
export async function record(params: recordParam): Promise<TelemetryUserResponse> {
	if (!params.taskId || !params.action) {
		return {}
	}
	const url = WECODE_PROXY_URL + UserActionEnum[params.action].url
	try {
		const headers = generateHeaders(params.action)
		if (params?.modelId) {
			headers["wecode-model-id"] = params?.modelId as string
		}
		const body = {
			id: params.id,
			session_id: params.taskId,
			action: params.action,
			type: params.action,
			language: "unknow",
			line_add_count: params?.lineAddCount,
			code_add_conents: params?.codeAddContents || [],
			line_delete_count: params?.deleteLineCount,
			code_delete_conents: params?.codeDeleteContents || [],
			filepath: params?.filePath,
			git_url: params?.gitUrl,
			mode: params?.mode,
		}

		if (params.language) {
			body["language"] = params.language
		}

		const response = await fetch(url, { method: "POST", headers: headers, body: JSON.stringify(body) })
		if (response.ok) {
			const result = response.statusText
			console.log("telemetryUserService result:", result, params.taskId, params.id, params.action)
			return {}
		} else {
			console.error(`telemetryUserService error: ${response.statusText}`)
		}
	} catch (error) {
		console.error("telemetryUserService error: ", error)
		return {}
	}
	return {}
}
