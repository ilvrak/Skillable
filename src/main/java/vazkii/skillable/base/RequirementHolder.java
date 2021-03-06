package vazkii.skillable.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import vazkii.skillable.lib.LibObfuscation;
import vazkii.skillable.skill.Skill;
import vazkii.skillable.skill.Skills;

public class RequirementHolder {

	public final Map<Skill, Integer> skillLevels = new HashMap();
	public final List<ResourceLocation> advancements = new ArrayList();

	boolean forcedEmpty = false;

	public static RequirementHolder realEmpty() {
		RequirementHolder h = new RequirementHolder();
		h.forcedEmpty = true;
		return h;
	}

	public boolean isRealLock() {
		return getRestrictionLength() > 0 && !forcedEmpty;
	}

	public boolean isForcedEmpty() {
		return forcedEmpty;
	}

	public int getRestrictionLength() {
		return skillLevels.size() + advancements.size();
	}

	@SideOnly(Side.CLIENT)
	public void addRequirementsToTooltip(PlayerData data, List<String> tooltip) {
		if (!isRealLock())
			return;

		if (GuiScreen.isShiftKeyDown()) {
			tooltip.add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("skillable.misc.skillLock"));

			for (Skill s : skillLevels.keySet()) {
				PlayerSkillInfo info = data.getSkillInfo(s);
				TextFormatting color = TextFormatting.GREEN;
				int req = skillLevels.get(s);
				if (info.getLevel() < req)
					color = TextFormatting.RED;

				tooltip.add(TextFormatting.GRAY + " - "
						+ I18n.translateToLocalFormatted("skillable.misc.skillFormat", color, req, s.getName()));
			}

			EntityPlayer p = data.playerWR.get();
			if (p != null)
				for (ResourceLocation e : advancements) {
					Advancement adv = getAdvancementList().getAdvancement(e);
					if(adv != null)
						tooltip.add(TextFormatting.GRAY + " - " + I18n.translateToLocalFormatted(
							"skillable.misc.achievementFormat", adv.getDisplayText().getUnformattedText().replaceAll("\\[|\\]", "")));
				}
		} else
			tooltip.add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("skillable.misc.skillLockShift"));
	}

	public static RequirementHolder fromString(String s) {
		if (s.matches("(?i)^(none|null|nil)$"))
			return RequirementHolder.realEmpty();

		RequirementHolder holder = new RequirementHolder();
		String[] tokens = s.trim().split(",");

		for (String s1 : tokens) {
			String[] kv = s1.split("\\|");
			if (kv.length == 2) {
				String keyStr = kv[0];
				String valStr = kv[1];

				if (keyStr.equals("adv"))
					holder.advancements.add(new ResourceLocation(valStr));
				else
					try {
						int level = Integer.parseInt(valStr);
						Skill skill = Skills.ALL_SKILLS.get(keyStr.toLowerCase());
						if (skill != null && level > 1)
							holder.skillLevels.put(skill, level);
						else
							FMLLog.warning("[Skillable] Invalid Level Lock: " + s);
					} catch (NumberFormatException e) {
						FMLLog.warning("[Skillable] Invalid Level Lock: " + s);
					}
			}
		}

		return holder;
	}
	
	private static AdvancementList advList;
	
	public static AdvancementList getAdvancementList() {
		if(advList == null)
			advList = ReflectionHelper.getPrivateValue(AdvancementManager.class, null, LibObfuscation.ADVANCEMENT_LIST);
		
		return advList;
	}

}
