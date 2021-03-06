package vazkii.skillable.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import vazkii.arl.network.NetworkHandler;
import vazkii.arl.util.RenderHelper;
import vazkii.skillable.base.ConfigHandler;
import vazkii.skillable.base.LevelLockHandler;
import vazkii.skillable.base.PlayerData;
import vazkii.skillable.base.PlayerDataHandler;
import vazkii.skillable.base.PlayerSkillInfo;
import vazkii.skillable.client.gui.button.GuiButtonLevelUp;
import vazkii.skillable.client.gui.handler.InventoryTabHandler;
import vazkii.skillable.lib.LibMisc;
import vazkii.skillable.network.MessageLevelUp;
import vazkii.skillable.network.MessageUnlockUnlockable;
import vazkii.skillable.skill.Skill;
import vazkii.skillable.skill.base.Unlockable;

public class GuiSkillInfo extends GuiScreen {

	public static final ResourceLocation SKILL_INFO_RES = new ResourceLocation(LibMisc.MOD_ID, "textures/gui/skill_info.png");
	
	private final Skill skill;
	
	int guiWidth, guiHeight;
	TextureAtlasSprite sprite;
	
	GuiButtonLevelUp levelUpButton;
	Unlockable hoveredUnlockable;
	boolean canPurchase;
	
	public GuiSkillInfo(Skill skill) {
		this.skill = skill;
	}
	
	@Override
	public void initGui() {
		guiWidth = 176;
		guiHeight = 166;
		
		int left = width / 2 - guiWidth / 2;
		int top = height / 2 - guiHeight / 2;
		
		buttonList.clear();
		buttonList.add(levelUpButton = new GuiButtonLevelUp(left + 147, top + 10));
		InventoryTabHandler.addTabs(this, buttonList);
		sprite = getTexture(skill.getBackground());
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		
		int left = width / 2 - guiWidth / 2;
		int top = height / 2 - guiHeight / 2;
		
		PlayerData data = PlayerDataHandler.get(mc.player);
		PlayerSkillInfo skillInfo = data.getSkillInfo(skill);
		
		mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.color(0.5F, 0.5F, 0.5F);
		for(int i = 0; i < 9; i++)
			for(int j = 0; j < 8; j++) {
				int x = left + 16 + i * 16;
				int y = top + 33 + j * 16;
				drawTexturedModalRect(x, y, sprite, 16, 16);
			}
		
		GlStateManager.color(1F, 1F, 1F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		mc.renderEngine.bindTexture(SKILL_INFO_RES);
		
		drawTexturedModalRect(left, top, 0, 0, guiWidth, guiHeight);
		
		GuiSkills.drawSkill(left + 15, top + 9, skill);
		
		String levelStr = String.format("%d/%d (%s)", skillInfo.getLevel(), ConfigHandler.levelCap, I18n.translateToLocal("skillable.rank." + skillInfo.getRank()));
		mc.fontRenderer.drawString(TextFormatting.BOLD + skill.getName(), left + 37, top + 8, 4210752);
		mc.fontRenderer.drawString(levelStr, left + 37, top + 18, 4210752);
		
		mc.fontRenderer.drawString(String.format(I18n.translateToLocal("skillable.misc.skillPoints"), skillInfo.getSkillPoints()), left + 15, top + 154, 4210752);
		
		int cost = skillInfo.getLevelUpCost();
		String costStr = Integer.toString(cost);
		if(skillInfo.isCapped())
			costStr = I18n.translateToLocal("skillable.misc.capped");
		
		levelUpButton.setCost(cost);
		
		drawCenteredString(mc.fontRenderer, costStr, left + 138, top + 13, 0xAFFF02);
		
		hoveredUnlockable = null;
		for(Unlockable u : skill.getUnlockables())
			drawUnlockable(data, skillInfo, u, mouseX, mouseY);
		
		super.drawScreen(mouseX, mouseY, partialTicks);
		
		if(hoveredUnlockable != null)
			makeUnlockableTooltip(data, skillInfo, mouseX, mouseY);
	}
	
	private void drawUnlockable(PlayerData data, PlayerSkillInfo info, Unlockable unlockable, int mx, int my) {
		int x = width / 2 - guiWidth / 2 + 20 + unlockable.x * 28;
		int y = height / 2 - guiHeight / 2 + 37 + unlockable.y * 28;
		mc.renderEngine.bindTexture(SKILL_INFO_RES);
		boolean unlocked = info.isUnlocked(unlockable);
		
		int u = 0;
		int v = guiHeight;
		if(unlockable.hasSpikes())
			u += 26;
		if(unlocked)
			v += 26;
	
		GlStateManager.color(1F, 1F, 1F);
		drawTexturedModalRect(x, y, u, v, 26, 26);
		
		mc.renderEngine.bindTexture(unlockable.getIcon());
		drawModalRectWithCustomSizedTexture(x + 5, y + 5, 0, 0, 16, 16, 16, 16);
		
		if(mx >= x && my >= y && mx < x + 26 && my < y + 26) {
			canPurchase = !unlocked && info.getSkillPoints() >= unlockable.cost;
			hoveredUnlockable = unlockable;
		}
	}
	
	private void makeUnlockableTooltip(PlayerData data, PlayerSkillInfo info, int mouseX, int mouseY) {
		List<String> tooltip = new ArrayList();
		TextFormatting tf = hoveredUnlockable.hasSpikes() ? TextFormatting.AQUA : TextFormatting.YELLOW;
		
		tooltip.add(tf + hoveredUnlockable.getName());
		
		if(isShiftKeyDown())
			addLongStringToTooltip(tooltip, hoveredUnlockable.getDescription(), guiWidth);
		else {
			tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("skillable.misc.holdShift"));
			tooltip.add("");
		}
		
		if(!info.isUnlocked(hoveredUnlockable))
			hoveredUnlockable.getRequirements().addRequirementsToTooltip(data, tooltip);
		else tooltip.add(TextFormatting.GREEN + I18n.translateToLocal("skillable.misc.unlocked"));
		tooltip.add(TextFormatting.GRAY + String.format(I18n.translateToLocal("skillable.misc.skillPoints"), hoveredUnlockable.cost));
		
		RenderHelper.renderTooltip(mouseX, mouseY, tooltip);
	}
	
	private void addLongStringToTooltip(List<String> tooltip, String longStr, int maxLen) {
		String[] tokens = longStr.split(" ");
		String curr = TextFormatting.GRAY.toString();
		int i = 0;

		while(i < tokens.length) {
			while(fontRenderer.getStringWidth(curr) < maxLen && i < tokens.length) {
				curr = curr + tokens[i] + " ";
				i++;
			}
			tooltip.add(curr);
			curr = TextFormatting.GRAY.toString();
		}
		
		tooltip.add(curr);
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(button == levelUpButton) {
			MessageLevelUp message = new MessageLevelUp(skill.getKey());
			NetworkHandler.INSTANCE.sendToServer(message);
		}
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(mouseButton == 0 && hoveredUnlockable != null && canPurchase) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			MessageUnlockUnlockable message = new MessageUnlockUnlockable(skill.getKey(), hoveredUnlockable.getKey());
			NetworkHandler.INSTANCE.sendToServer(message);
		} else if(mouseButton == 1 || mouseButton == 3)
			mc.displayGuiScreen(new GuiSkills());
	}
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
	
    private TextureAtlasSprite getTexture(Block blockIn) {
        return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(blockIn.getDefaultState());
    }
	
}
