import { ExtensionMessage } from "@roo/ExtensionMessage"

export function handleWeCoderMessages(message: ExtensionMessage) {
	switch (message.type) {
		case "externalRiskNotice": {
			if (message.values?.gptWarning && message.values?.pendingApiConfigId) {
				window.postMessage(
					{
						type: "externalRiskNoticeUpdate",
						values: {
							source: "backend",
							gptWarning: true,
							pendingApiConfigId: message.values.pendingApiConfigId,
						},
					},
					"*",
				)
				break
			}
			break
		}
	}
}
