import Tkinter;
from PIL import Image, ImageTk

import os
import glob

dir = './ogamebotapp/src/main/resources/drivers/Quantum_bc3ew9p4yh9qdv8wvj1h/images/';

root = Tkinter.Tk()
label = Tkinter.Label(root)
label.pack()
img = None
tkimg = [None]  # This, or something like it, is necessary because if you do not keep a reference to PhotoImage instances, they get garbage collected.

delay = 500   # refresh image time in milliseconds
def loopCapture():
    #TODO update label to be that of the current image see display.py
    #get newest image in directory given
    img = Image.open(max(glob.iglob(dir+'*.[Pp][Nn][Gg]'), key=os.path.getctime));
    tkimg[0] = ImageTk.PhotoImage(img)
    label.config(image=tkimg[0])
    root.update_idletasks()
    root.after(delay, loopCapture)

loopCapture()
root.mainloop()
