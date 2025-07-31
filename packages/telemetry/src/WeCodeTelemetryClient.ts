import * as vscode from "vscode"

import { BaseTelemetryClient } from "./BaseTelemetryClient"
import { TelemetryEventName, type TelemetryEvent } from "@roo-code/types"
import { getCache } from "../../../src/wecoder/utils/cache/taskCache"
import { generateHeaders } from "../../../src/wecoder/utils/wecode-headers"
import { UserActionEnum } from "../../../src/wecoder/utils/userAction"
import { WECODE_PROXY_URL } from "./wecoder_constants"

/**
 * WeCodeTelemetryClient handles telemetry event tracking for the WeCode extension.
 * Uses WeCode's internal telemetry service to track user interactions and system events.
 * Respects user privacy settings and VSCode's global telemetry configuration.
 */
export class WeCodeTelemetryClient extends BaseTelemetryClient {
	private distinctId: string = vscode.env.machineId
	private action = "wecoder_telemetry"
	private url = WECODE_PROXY_URL + UserActionEnum[this.action].url

	constructor(debug = false) {
		super(
			{
				type: "exclude",
				events: [TelemetryEventName.TASK_MESSAGE, TelemetryEventName.LLM_COMPLETION],
			},
			debug,
		)
	}

	public override async capture(event: TelemetryEvent): Promise<void> {
		// 使用 WeCodeTelemetryService 的 capture 方法
		await this.doCapture({
			event: event.event,
			properties: await this.getEventProperties(event),
		})
	}

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	public async doCapture(event: { event: string; properties?: Record<string, any> }): Promise<void> {
		try {
			// eslint-disable-next-line @typescript-eslint/no-explicit-any
			let globalProperties: Record<string, any> = {}
			const provider = this.providerRef?.deref()
			if (provider) {
				globalProperties = await provider.getTelemetryProperties()
			}

			const mergedProperties = { ...globalProperties, ...(event.properties || {}) }

			const body = JSON.stringify({
				action: this.action,
				event: event.event,
				distinct_id: this.distinctId,
				properties: mergedProperties,
			})

			const headers = generateHeaders(this.action)

			// 为了解决模型id是xxx-auto的问题, 尝试从缓存中获取任务对应的模型;
			const cache = getCache(mergedProperties?.taskId)
			if (cache && cache?.modelId) {
				headers["wecode-model-id"] = cache.modelId
				mergedProperties.modelId = cache.modelId
			}

			const response = await fetch(this.url, { method: "POST", headers: headers, body: body })
			if (response.ok) {
				const result = response.statusText
				console.log("http telemetry Service capture capture result:", result)
			} else {
				console.error(`http telemetry Service capture error: ${response.statusText}`)
			}
		} catch (error) {
			console.error("http telemetry Service capture failed:", error)
		}
	}

	/**
	 * Updates the telemetry state based on user preferences and VSCode settings.
	 * Only enables telemetry if both VSCode global telemetry is enabled and
	 * user has opted in.
	 * @param didUserOptIn Whether the user has explicitly opted into telemetry
	 */
	public override updateTelemetryState(didUserOptIn: boolean): void {
		this.telemetryEnabled = false

		// First check global telemetry level - telemetry should only be enabled when level is "all".
		const telemetryLevel = vscode.workspace.getConfiguration("telemetry").get<string>("telemetryLevel", "all")
		const globalTelemetryEnabled = telemetryLevel === "all"

		// We only enable telemetry if global vscode telemetry is enabled.
		if (globalTelemetryEnabled) {
			this.telemetryEnabled = didUserOptIn
		}

		// Update WeCode service telemetry state
	}

	public async shutdown(): Promise<void> {
		return
		// HTTP方式无需关闭，直接返回
	}

	public async captureDiffRecorderError(taskId: string, consecutiveMistakeCount: number): Promise<void> {
		await this.capture({
			event: TelemetryEventName.WECODER_DIFF_RECORDER_ERROR,
			properties: { taskId, consecutiveMistakeCount },
		})
	}

	public async captureDiffApplicationSuccess(taskId: string, consecutiveMistakeCount: number): Promise<void> {
		await this.capture({
			event: TelemetryEventName.WECODER_DIFF_APPLICATION_SUCCESS,
			properties: { taskId, consecutiveMistakeCount },
		})
	}

	/**
	 * 上报重复内容检测事件
	 * @param taskId 任务ID
	 * @param retryCount 当前重试次数
	 * @param detectionMethod 检测方法或阶段（可选）
	 */
	public async captureDuplicateContentDetected(
		taskId: string,
		retryCount: number,
		detectionMethod?: string,
	): Promise<void> {
		await this.capture({
			event: TelemetryEventName.WECODER_DUPLICATE_CONTENT_DETECTED,
			properties: {
				taskId,
				retryCount,
				detectionMethod,
			},
		})
	}
}

export const weCodeTelemetryClient = new WeCodeTelemetryClient(false)
