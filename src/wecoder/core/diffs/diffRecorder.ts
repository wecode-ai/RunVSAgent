import getGitRemoteUrl from "../../utils/getGitId"
import { getCache } from "../../utils/cache/taskCache"
import * as diff from "diff"
import { record, recordParam } from "../../utils/telemetry"
import { weCodeTelemetryClient } from "@roo-code/telemetry"
export async function recordWecodeDiffs(
	originalContent: string | undefined,
	currentContent: string,
	absolutePath: string,
	taskId: string,
) {
	const diffs = diff.diffLines(originalContent || "", currentContent)
	if (!diff) {
		weCodeTelemetryClient.captureDiffRecorderError(taskId, 0)
		return
	}

	let taskMeta = getCache(taskId)
	// 临时支持一下，如果 taskMeta 不存在，就创建一个
	if (!taskMeta) {
		taskMeta = {
			id: taskId,
			modelId: "custom_model_id",
			taskMessage: "",
		}
	}

	let lineCount = 0
	let lineContent: string[] = []
	let deleteLineCount = 0
	let deletedLineContent: string[] = []

	for (const diff of diffs) {
		if (diff?.added && diff?.count) {
			lineCount = lineCount + diff.count
			lineContent.push(diff.value)
		}

		if (diff?.removed && diff?.count) {
			deleteLineCount = diff.count + deleteLineCount
			deletedLineContent.push(diff.value)
		}
	}

	const gitUrl = getGitRemoteUrl()
	const params: recordParam = {
		id: taskMeta?.id,
		modelId: taskMeta?.modelId,
		mode: taskMeta?.mode,
		taskId: taskId,
		action: "wecoder_save",
		lineAddCount: lineCount,
		filePath: absolutePath,
		codeAddContents: lineContent,
		deleteLineCount: deleteLineCount,
		codeDeleteContents: deletedLineContent,
		gitUrl: gitUrl,
	}

	record(params)
}
