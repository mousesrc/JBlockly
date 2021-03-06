package com.github.mousesrc.jblockly.fx.util;

import java.io.Reader;
import com.github.mousesrc.jblockly.fx.FXBlock;
import com.github.mousesrc.jblockly.fx.FXBlockRow;
import com.github.mousesrc.jblockly.fx.FXBlockWorkspace;
import com.github.mousesrc.jblockly.fx.input.Inputer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.scene.Node;

public class WorkspaceStorage {

	public static final Gson GSON = new GsonBuilder().create();

	public static void loadFromJson(FXBlockWorkspace workspace, String json) {
		loadFromJsonTree(workspace, GSON.fromJson(json, JsonElement.class));
	}

	public static void loadFromJson(FXBlockWorkspace workspace, Reader reader) {
		loadFromJsonTree(workspace, GSON.fromJson(reader, JsonElement.class));
	}

	public static void loadFromJsonTree(FXBlockWorkspace workspace, JsonElement element) {
		workspace.getBlocks().clear();
		JsonObject object = element.getAsJsonObject();
		JsonArray blocks = object.get("blocks").getAsJsonArray();
		for (JsonElement blockElement : blocks) {
			JsonObject blockObject = blockElement.getAsJsonObject();
			FXBlock fxBlock = FXBlockSerializer.loadFromJsonTree(workspace, blockObject);
			fxBlock.setLayoutX(blockObject.get("x").getAsDouble());
			fxBlock.setLayoutY(blockObject.get("y").getAsDouble());
			workspace.getBlocks().add(fxBlock);
		}
	}

	public static String saveToJson(FXBlockWorkspace workspace) {
		return GSON.toJson(saveToJsonTree(workspace));
	}

	public static void saveToJson(FXBlockWorkspace workspace, Appendable writer) {
		GSON.toJson(saveToJsonTree(workspace), writer);
	}

	public static JsonElement saveToJsonTree(FXBlockWorkspace workspace) {
		JsonObject root = new JsonObject();
		JsonArray blocks = new JsonArray();
		for (FXBlock block : workspace.getBlocks()) {
			JsonObject jsonBlock = FXBlockSerializer.saveToJsonTree(block);
			jsonBlock.addProperty("x", block.getLayoutX());
			jsonBlock.addProperty("y", block.getLayoutY());
			blocks.add(jsonBlock);
		}
		root.add("blocks", blocks);
		return root;
	}

	public static class FXBlockSerializer {

		public static JsonObject saveToJsonTree(FXBlock block) {
			JsonObject object = new JsonObject();
			object.addProperty("name", block.getName());
			
			JsonObject rows = new JsonObject();
			for (FXBlockRow row : block.getFXRows()) {
				String name = row.getName();
				if (name == null)
					continue;
				rows.add(name, FXBlockRowSerializer.saveToJsonTree(row));
			}
			object.add("rows", rows);
			return object;
		}

		public static FXBlock loadFromJsonTree(FXBlockWorkspace workspace, JsonObject object) {
			if (!object.has("name"))
				return null;

			BlockProvider provider = workspace.getBlockRegistry().get(object.get("name").getAsString());
			if (provider == null)
				return null;

			FXBlock block = provider.create();
			JsonObject rows = object.getAsJsonObject("rows");
			for (FXBlockRow row : block.getFXRows())
				if (row.getName() != null && rows.has(row.getName()))
					FXBlockRowSerializer.loadFromJsonTree(workspace, row, rows.getAsJsonObject(row.getName()));

			return block;
		}
	}

	public static class FXBlockRowSerializer {

		public static JsonObject saveToJsonTree(FXBlockRow row) {
			JsonObject object = new JsonObject();
			FXBlock fxBlock = row.getFXBlock();
			if (fxBlock != null) {
				object.add("block", FXBlockSerializer.saveToJsonTree(fxBlock));
			}
			JsonObject datas = new JsonObject();
			for (Node node : row.getComponents()) {
				if (!(node instanceof Inputer<?>))
					continue;
				
				Inputer<?> inputer = (Inputer<?>) node;
				if (inputer.getName() == null)
					continue;

				Object value = inputer.getValue();
				if (value == null)
					continue;

				JsonObject data = new JsonObject();
				data.addProperty("type", value.getClass().getName());
				data.add("value", GSON.toJsonTree(value));
				datas.add(inputer.getName(), data);
			}
			if (datas.size() > 0) {
				object.add("data", datas);
			}
			return object;
		}

		public static void loadFromJsonTree(FXBlockWorkspace workspace, FXBlockRow row, JsonObject object) {
			if (object.has("block")) {
				row.setBlock(FXBlockSerializer.loadFromJsonTree(workspace, object.getAsJsonObject("block")));
			}
			if (object.has("data")) {
				JsonObject datas = object.getAsJsonObject("data");
				for (Node node : row.getComponents()) {
					if (!(node instanceof Inputer<?>))
						continue;
					
					Inputer<?> inputer = (Inputer<?>) node;
					if (inputer.getName() == null || !datas.has(inputer.getName()))
						continue;

					JsonObject data = datas.getAsJsonObject(inputer.getName());
					try {
						inputer.set(GSON.fromJson(data.get("value"), Class.forName(data.get("type").getAsString())));
					} catch (ClassNotFoundException ignored) {
					}
				}
			}
		}
	}
}
