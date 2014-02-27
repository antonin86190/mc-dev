package net.minecraft.server;

public class TileEntityFurnace extends TileEntity implements IWorldInventory {

    private static final int[] k = new int[] { 0};
    private static final int[] l = new int[] { 2, 1};
    private static final int[] m = new int[] { 1};
    private ItemStack[] items = new ItemStack[3];
    public int burnTime;
    public int ticksForCurrentFuel;
    public int cookTime;
    private String o;

    public TileEntityFurnace() {}

    public int getSize() {
        return this.items.length;
    }

    public ItemStack getItem(int i) {
        return this.items[i];
    }

    public ItemStack splitStack(int i, int j) {
        if (this.items[i] != null) {
            ItemStack itemstack;

            if (this.items[i].count <= j) {
                itemstack = this.items[i];
                this.items[i] = null;
                return itemstack;
            } else {
                itemstack = this.items[i].a(j);
                if (this.items[i].count == 0) {
                    this.items[i] = null;
                }

                return itemstack;
            }
        } else {
            return null;
        }
    }

    public ItemStack splitWithoutUpdate(int i) {
        if (this.items[i] != null) {
            ItemStack itemstack = this.items[i];

            this.items[i] = null;
            return itemstack;
        } else {
            return null;
        }
    }

    public void setItem(int i, ItemStack itemstack) {
        this.items[i] = itemstack;
        if (itemstack != null && itemstack.count > this.getMaxStackSize()) {
            itemstack.count = this.getMaxStackSize();
        }
    }

    public String getInventoryName() {
        return this.k_() ? this.o : "container.furnace";
    }

    public boolean k_() {
        return this.o != null && this.o.length() > 0;
    }

    public void a(String s) {
        this.o = s;
    }

    public void a(NBTTagCompound nbttagcompound) {
        super.a(nbttagcompound);
        NBTTagList nbttaglist = nbttagcompound.getList("Items", 10);

        this.items = new ItemStack[this.getSize()];

        for (int i = 0; i < nbttaglist.size(); ++i) {
            NBTTagCompound nbttagcompound1 = nbttaglist.get(i);
            byte b0 = nbttagcompound1.getByte("Slot");

            if (b0 >= 0 && b0 < this.items.length) {
                this.items[b0] = ItemStack.createStack(nbttagcompound1);
            }
        }

        this.burnTime = nbttagcompound.getShort("BurnTime");
        this.cookTime = nbttagcompound.getShort("CookTime");
        this.ticksForCurrentFuel = fuelTime(this.items[1]);
        if (nbttagcompound.hasKeyOfType("CustomName", 8)) {
            this.o = nbttagcompound.getString("CustomName");
        }
    }

    public void b(NBTTagCompound nbttagcompound) {
        super.b(nbttagcompound);
        nbttagcompound.setShort("BurnTime", (short) this.burnTime);
        nbttagcompound.setShort("CookTime", (short) this.cookTime);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.items.length; ++i) {
            if (this.items[i] != null) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                nbttagcompound1.setByte("Slot", (byte) i);
                this.items[i].save(nbttagcompound1);
                nbttaglist.add(nbttagcompound1);
            }
        }

        nbttagcompound.set("Items", nbttaglist);
        if (this.k_()) {
            nbttagcompound.setString("CustomName", this.o);
        }
    }

    public int getMaxStackSize() {
        return 64;
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    public void h() {
        boolean flag = this.burnTime > 0;
        boolean flag1 = false;

        if (this.burnTime > 0) {
            --this.burnTime;
        }

        if (!this.world.isStatic) {
            if (this.burnTime != 0 || this.items[1] != null && this.items[0] != null) {
                if (this.burnTime == 0 && this.canBurn()) {
                    this.ticksForCurrentFuel = this.burnTime = fuelTime(this.items[1]);
                    if (this.burnTime > 0) {
                        flag1 = true;
                        if (this.items[1] != null) {
                            --this.items[1].count;
                            if (this.items[1].count == 0) {
                                Item item = this.items[1].getItem().t();

                                this.items[1] = item != null ? new ItemStack(item) : null;
                            }
                        }
                    }
                }

                if (this.isBurning() && this.canBurn()) {
                    ++this.cookTime;
                    if (this.cookTime == 200) {
                        this.cookTime = 0;
                        this.burn();
                        flag1 = true;
                    }
                } else {
                    this.cookTime = 0;
                }
            }

            if (flag != this.burnTime > 0) {
                flag1 = true;
                BlockFurnace.a(this.burnTime > 0, this.world, this.x, this.y, this.z);
            }
        }

        if (flag1) {
            this.update();
        }
    }

    private boolean canBurn() {
        if (this.items[0] == null) {
            return false;
        } else {
            ItemStack itemstack = RecipesFurnace.getInstance().getResult(this.items[0]);

            return itemstack == null ? false : (this.items[2] == null ? true : (!this.items[2].doMaterialsMatch(itemstack) ? false : (this.items[2].count < this.getMaxStackSize() && this.items[2].count < this.items[2].getMaxStackSize() ? true : this.items[2].count < itemstack.getMaxStackSize())));
        }
    }

    public void burn() {
        if (this.canBurn()) {
            ItemStack itemstack = RecipesFurnace.getInstance().getResult(this.items[0]);

            if (this.items[2] == null) {
                this.items[2] = itemstack.cloneItemStack();
            } else if (this.items[2].getItem() == itemstack.getItem()) {
                ++this.items[2].count;
            }

            --this.items[0].count;
            if (this.items[0].count <= 0) {
                this.items[0] = null;
            }
        }
    }

    public static int fuelTime(ItemStack itemstack) {
        if (itemstack == null) {
            return 0;
        } else {
            Item item = itemstack.getItem();

            if (item instanceof ItemBlock && Block.a(item) != Blocks.AIR) {
                Block block = Block.a(item);

                if (block == Blocks.WOOD_STEP) {
                    return 150;
                }

                if (block.getMaterial() == Material.WOOD) {
                    return 300;
                }

                if (block == Blocks.COAL_BLOCK) {
                    return 16000;
                }
            }

            return item instanceof ItemTool && ((ItemTool) item).j().equals("WOOD") ? 200 : (item instanceof ItemSword && ((ItemSword) item).j().equals("WOOD") ? 200 : (item instanceof ItemHoe && ((ItemHoe) item).i().equals("WOOD") ? 200 : (item == Items.STICK ? 100 : (item == Items.COAL ? 1600 : (item == Items.LAVA_BUCKET ? 20000 : (item == Item.getItemOf(Blocks.SAPLING) ? 100 : (item == Items.BLAZE_ROD ? 2400 : 0)))))));
        }
    }

    public static boolean isFuel(ItemStack itemstack) {
        return fuelTime(itemstack) > 0;
    }

    public boolean a(EntityHuman entityhuman) {
        return this.world.getTileEntity(this.x, this.y, this.z) != this ? false : entityhuman.e((double) this.x + 0.5D, (double) this.y + 0.5D, (double) this.z + 0.5D) <= 64.0D;
    }

    public void startOpen() {}

    public void l_() {}

    public boolean b(int i, ItemStack itemstack) {
        return i == 2 ? false : (i == 1 ? isFuel(itemstack) : true);
    }

    public int[] getSlotsForFace(int i) {
        return i == 0 ? l : (i == 1 ? k : m);
    }

    public boolean canPlaceItemThroughFace(int i, ItemStack itemstack, int j) {
        return this.b(i, itemstack);
    }

    public boolean canTakeItemThroughFace(int i, ItemStack itemstack, int j) {
        return j != 0 || i != 1 || itemstack.getItem() == Items.BUCKET;
    }
}
