import { useState, memo } from "react"

import { Package } from "@roo/package"

import { useAppTranslation } from "@src/i18n/TranslationContext"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@src/components/ui"

interface AnnouncementProps {
	hideAnnouncement: () => void
}

/**
 * You must update the `latestAnnouncementId` in ClineProvider for new
 * announcements to show to users. This new id will be compared with what's in
 * state for the 'last announcement shown', and if it's different then the
 * announcement will render. As soon as an announcement is shown, the id will be
 * updated in state. This ensures that announcements are not shown more than
 * once, even if the user doesn't close it themselves.
 */

const Announcement = ({ hideAnnouncement }: AnnouncementProps) => {
	const { t } = useAppTranslation()
	const [open, setOpen] = useState(true)

	return (
		<Dialog
			open={open}
			onOpenChange={(open) => {
				setOpen(open)

				if (!open) {
					hideAnnouncement()
				}
			}}>
			<DialogContent className="max-w-96">
				<DialogHeader>
					<DialogTitle>{t("chat:announcement.title", { version: Package.version })}</DialogTitle>
					<DialogDescription>
						{t("chat:announcement.description", { version: Package.version })}
					</DialogDescription>
				</DialogHeader>
				<div>
					<h3>{t("chat:announcement.whatsNew")}</h3>
					<ul className="space-y-2">
						{/* Announcement updated: support for Kimi K2 model */}
						<li>
							已支持内网Kimi K2模型，欢迎测试&nbsp;
							<span role="img" aria-label="celebration">
								🎉
							</span>
						</li>
					</ul>
				</div>
			</DialogContent>
		</Dialog>
	)
}

export default memo(Announcement)
